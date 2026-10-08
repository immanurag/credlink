package com.credlink.nlp;

import com.credlink.nlp.dto.NlpExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Calls an OpenAI/OpenRouter-compatible chat-completions endpoint to extract a structured
 * transaction from a free-form Hindi/Hinglish voice transcript. The API key lives only in
 * backend config (credlink.nlp.api-key) and is never sent to or readable by the frontend.
 */
@Service
public class LiveLlmNlpService {

    private static final Logger log = LoggerFactory.getLogger(LiveLlmNlpService.class);

    private static final String SYSTEM_PROMPT = """
            You are a transaction extractor and translator for a Kirana (small retail) store credit ledger app.
            The merchant speaks in Hindi, Hinglish, or English about a customer transaction.
            Extract exactly one JSON object with this shape and nothing else:
            {
              "customerName": "<customer name in English script, e.g. Mohan, Rahul, Sonu, Aditya>",
              "amount": <number>,
              "type": "UDHAAR" or "JAMA",
              "outstandingAmount": <number or null if not stated>,
              "englishDescription": "<clear, natural, grammatically correct English summary of the transaction. MUST BE PROPER ENGLISH ONLY. Do NOT use Hinglish or Roman Hindi. Examples: 'I received 500 rupees from Aditya.', 'Payment of 500 rupees received from Aditya (Outstanding balance: 200 rupees).'>"
            }
            CRITICAL RULES:
            1. Do NOT sum or add multiple monetary amounts together! If the user mentions a payment/credit amount AND an outstanding/due amount (e.g. '500 rupaye jama kiye, 200 rupaye baki'), set amount = 500, type = JAMA, and outstandingAmount = 200. Never calculate 500 + 200 = 700.
            2. UDHAAR = credit given to customer / money customer owes merchant (increases debt).
            3. JAMA = payment received from customer / money merchant owes customer (decreases debt).
            If you cannot confidently extract all required fields, respond with {"error": "reason"}.
            """;

    private final RestTemplate restTemplate = new RestTemplate();
    private final LlmResponseParser parser;

    @Value("${credlink.nlp.api-key}")
    private String apiKey;

    @Value("${credlink.nlp.api-url}")
    private String apiUrl;

    @Value("${credlink.nlp.model}")
    private String model;

    public LiveLlmNlpService(LlmResponseParser parser) {
        this.parser = parser;
    }

    public NlpExtractionResult extract(String transcript) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LLM live mode is enabled but credlink.nlp.api-key is not set.");
            return NlpExtractionResult.invalid("LLM API key is not configured.");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", transcript)
                    ),
                    "temperature", 0
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(apiUrl, entity, String.class);
            if (response == null) {
                return NlpExtractionResult.invalid("LLM provider returned an empty response.");
            }
            return parser.parseCompletionEnvelope(response);
        } catch (Exception e) {
            log.error("LLM extraction call failed: {}", e.getMessage());
            return NlpExtractionResult.invalid("LLM provider call failed: " + e.getMessage());
        }
    }
}
