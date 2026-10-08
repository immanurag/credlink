package com.credlink.nlp;

import com.credlink.nlp.dto.NlpExtractionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an OpenAI/OpenRouter-compatible chat completion response
 * (choices[0].message.content) into a strict NlpExtractionResult.
 * Kept separate from the HTTP call itself so it can be unit tested against
 * canned JSON without any network dependency.
 */
@Component
public class LlmResponseParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[^{}]*\\}", Pattern.DOTALL);

    private final ObjectMapper mapper = new ObjectMapper();

    /** Parses the raw HTTP response body from the chat-completions endpoint. */
    public NlpExtractionResult parseCompletionEnvelope(String rawBody) {
        try {
            JsonNode root = mapper.readTree(rawBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return NlpExtractionResult.invalid("LLM returned no choices.");
            }
            String content = choices.get(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                return NlpExtractionResult.invalid("LLM returned empty content.");
            }
            return parseExtractedJson(content);
        } catch (Exception e) {
            return NlpExtractionResult.invalid("Could not parse LLM response: " + e.getMessage());
        }
    }

    /** Parses the model's own JSON payload, e.g. {"customerName":"Ramesh","amount":200,"type":"CREDIT"} */
    public NlpExtractionResult parseExtractedJson(String content) {
        String jsonText = extractJsonBlock(content);
        if (jsonText == null) {
            return NlpExtractionResult.invalid("No JSON object found in LLM output.");
        }
        try {
            JsonNode node = mapper.readTree(jsonText);
            String customerName = textOrNull(node, "customerName");
            String typeRaw = textOrNull(node, "type");
            JsonNode amountNode = node.path("amount");

            if (customerName == null || typeRaw == null || amountNode.isMissingNode()) {
                return NlpExtractionResult.invalid("LLM JSON is missing required fields.");
            }

            double amount = amountNode.asDouble();
            if (amount <= 0) {
                return NlpExtractionResult.invalid("LLM extracted a non-positive amount.");
            }

            String normalizedType = normalizeType(typeRaw);
            if (normalizedType == null) {
                return NlpExtractionResult.invalid("LLM returned an unrecognized transaction type: " + typeRaw);
            }

            Double outstandingAmount = node.hasNonNull("outstandingAmount") ? node.path("outstandingAmount").asDouble() : null;

            String englishDescription = textOrNull(node, "englishDescription");
            if (englishDescription == null || englishDescription.isBlank()) {
                String formattedAmount = String.format("%.0f", amount);
                englishDescription = "UDHAAR".equals(normalizedType)
                        ? "Credit of " + formattedAmount + " rupees given to " + customerName + "."
                        : "Payment of " + formattedAmount + " rupees received from " + customerName + ".";
                if (outstandingAmount != null && !outstandingAmount.equals(amount)) {
                    englishDescription += " (Outstanding balance: " + String.format("%.0f", outstandingAmount) + " rupees).";
                }
            }

            return NlpExtractionResult.of(customerName, amount, normalizedType, outstandingAmount, 0.9, englishDescription);
        } catch (Exception e) {
            return NlpExtractionResult.invalid("Malformed JSON from LLM: " + e.getMessage());
        }
    }

    private String extractJsonBlock(String content) {
        Matcher m = JSON_BLOCK.matcher(content);
        return m.find() ? m.group() : null;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    /** Never trust raw LLM output directly - map it onto the two enum values we actually support. */
    private String normalizeType(String raw) {
        String upper = raw.trim().toUpperCase();
        if (upper.contains("CREDIT") || upper.contains("UDHAAR") || upper.equals("DEBIT_OWED")) return "UDHAAR";
        if (upper.contains("JAMA") || upper.contains("PAYMENT") || upper.contains("DEBIT")) return "JAMA";
        if (upper.equals("UDHAAR") || upper.equals("JAMA")) return upper;
        return null;
    }
}
