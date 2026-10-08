package com.credlink.nlp;

import com.credlink.nlp.dto.NlpExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, no-external-dependency extractor used when credlink.nlp.mode=mock, or as an
 * automatic fallback if the live LLM provider is unavailable.
 * Handles Hindi Unicode, Hinglish, and English phrasing, returning a strict NlpExtractionResult
 * with normalized English-only descriptions.
 */
@Service
public class RuleBasedNlpService {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedNlpService.class);

    private static final List<String> OUTSTANDING_KEYWORDS = Arrays.asList(
            "बकाया", "बकाया है", "बकाया हैं", "बाकी", "बाकी है", "बाकी हैं", "उधार बाकी", "देना बाकी", "रह गया", "उधार", "लेना है", "लेने हैं",
            "baki", "baaki", "bakaya", "due", "outstanding", "pending", "remaining", "left", "dena baki", "udhaar baki", "udhar", "udhaar", "paise baki", "lena hai"
    );

    private static final List<String> BALANCE_QUERY_KEYWORDS = Arrays.asList(
            "कितना", "कितने", "कितनी", "क्या", "बताओ", "दिखाओ", "चेक", "बताएं", "दिखाएं",
            "kitna", "kitne", "kitni", "kya", "batao", "dikhao", "check", "how much", "what is", "tell"
    );

    private static final List<String> TRANSACTION_ACTION_KEYWORDS = Arrays.asList(
            "जमा किया", "जमा किए", "जमा कर दिया", "भुगतान किया", "पेमेंट किया", "उधार दिया", "उधार दिए",
            "jama kiya", "jama kare", "jama kiye", "pay kiya", "paid", "deposited", "udhaar diya", "udhar diya"
    );

    private static final List<String> JAMA_KEYWORDS = Arrays.asList(
            "जमा किया", "जमा किए", "जमा कर दिया", "जमा", "भुगतान किया", "भुगतान", "पेमेंट किया", "पेमेंट", "लिए", "लिया", "मिला",
            "jama", "payment", "paid", "pay kiya", "pay kiye", "pay kar", "received", "deposited", "mila", "liya", "liye", "wapas"
    );

    private static final List<String> UDHAAR_KEYWORDS = Arrays.asList(
            "उधार दिए", "उधार दिया", "उधार", "udhaar", "udhar", "credit", "diya", "diye", "lene", "लेने", "देना"
    );

    // Captures name token before "se", "ko", "ka", "ki", "ke", "ne" in Latin or Devanagari
    private static final Pattern LATIN_NAME_PATTERN = Pattern.compile(
            "([A-Za-z]+)\\s+(?:se|ko|ka|ki|ke|ne)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEVANAGARI_NAME_PATTERN = Pattern.compile(
            "([\u0900-\u097F]+)\\s+(?:से|को|का|की|के|ने)(?:\\s+|$)");

    private static final Map<String, String> HINDI_NAME_MAP = new HashMap<>();
    static {
        HINDI_NAME_MAP.put("शिवांश", "Shivansh");
        HINDI_NAME_MAP.put("शिवास", "Shivas");
        HINDI_NAME_MAP.put("शिवा", "Shiva");
        HINDI_NAME_MAP.put("शिवम", "Shivam");
        HINDI_NAME_MAP.put("विशाल", "Vishal");
        HINDI_NAME_MAP.put("आदित्य", "Aditya");
        HINDI_NAME_MAP.put("मोहन", "Mohan");
        HINDI_NAME_MAP.put("राहुल", "Rahul");
        HINDI_NAME_MAP.put("सोनू", "Sonu");
        HINDI_NAME_MAP.put("रमेश", "Ramesh");
        HINDI_NAME_MAP.put("सुरेश", "Suresh");
        HINDI_NAME_MAP.put("अमित", "Amit");
        HINDI_NAME_MAP.put("अनिल", "Anil");
        HINDI_NAME_MAP.put("विकास", "Vikas");
        HINDI_NAME_MAP.put("रोहित", "Rohit");
        HINDI_NAME_MAP.put("दीपक", "Deepak");
        HINDI_NAME_MAP.put("पवन", "Pawan");
        HINDI_NAME_MAP.put("विजय", "Vijay");
        HINDI_NAME_MAP.put("राकेश", "Rakesh");
        HINDI_NAME_MAP.put("किशन", "Kishan");
        HINDI_NAME_MAP.put("सुनील", "Sunil");
    }

    public NlpExtractionResult extract(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return NlpExtractionResult.invalid("Transcript was empty.");
        }

        String normalized = HindiNumberParser.normalizeWordNumbersToDigits(transcript);
        String lower = normalized.toLowerCase();

        boolean hasQueryWord = BALANCE_QUERY_KEYWORDS.stream().anyMatch(lower::contains);
        boolean hasOutstandingWord = OUTSTANDING_KEYWORDS.stream().anyMatch(lower::contains);
        boolean hasStatementWord = lower.contains("statement") || lower.contains("स्टेटमेंट") || lower.contains("हिस्ट्री") || lower.contains("history");
        boolean hasTxAction = TRANSACTION_ACTION_KEYWORDS.stream().anyMatch(lower::contains);
        List<Double> amounts = HindiNumberParser.extractAmounts(transcript);

        // Detect STATEMENT_QUERY intent
        if ((hasStatementWord || (hasQueryWord && (lower.contains("khata") || lower.contains("खाता")))) && (!hasTxAction || amounts.isEmpty())) {
            String customerName = extractName(transcript);
            if (customerName == null) {
                customerName = extractName(normalized);
            }
            if (customerName == null) {
                customerName = "Customer";
            }
            double confidence = 0.95;
            String engDesc = "Statement query for customer " + customerName + ".";
            return NlpExtractionResult.statementQueryResult(customerName, confidence, engDesc);
        }

        // Detect OUTSTANDING_BALANCE_QUERY intent
        if (hasQueryWord && hasOutstandingWord && (!hasTxAction || amounts.isEmpty())) {
            String customerName = extractName(transcript);
            if (customerName == null) {
                customerName = extractName(normalized);
            }
            if (customerName == null) {
                customerName = "Customer";
            }
            double confidence = 0.95;
            String engDesc = "Balance inquiry for customer " + customerName + ".";
            return NlpExtractionResult.queryResult(customerName, confidence, engDesc);
        }

        if (amounts.isEmpty()) {
            return NlpExtractionResult.invalid("Could not detect an amount or valid query in the transcript.");
        }

        String customerName = extractName(transcript);
        if (customerName == null) {
            customerName = extractName(normalized);
        }
        if (customerName == null) {
            customerName = "Customer";
        }

        // Clause-based semantic extraction
        ExtractionContext ctx = extractContextFromClauses(normalized, amounts);
        if (ctx.amount == null && ctx.outstandingAmount == null) {
            return NlpExtractionResult.invalid("Could not determine transaction details from the transcript.");
        }

        Double amount = ctx.amount != null ? ctx.amount : ctx.outstandingAmount;
        String type = ctx.type != null ? ctx.type : "UDHAAR";
        Double outstandingAmount = ctx.outstandingAmount;

        String englishDescription = generateEnglishDescription(transcript, customerName, amount, type, outstandingAmount);

        double confidence = 0.85;
        if (transcript.matches(".*\\d+.*") || transcript.matches(".*[\u0966-\u096F]+.*")) confidence += 0.1;
        confidence = Math.min(confidence, 0.98);

        return NlpExtractionResult.of(customerName, amount, type, outstandingAmount, confidence, englishDescription);
    }

    private static class AmountOccurrence {
        double value;
        int startIndex;
        int endIndex;

        AmountOccurrence(double value, int startIndex, int endIndex) {
            this.value = value;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    private List<AmountOccurrence> findAmountOccurrences(String text) {
        List<AmountOccurrence> list = new ArrayList<>();
        Pattern p = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");
        Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                if (val > 0) {
                    list.add(new AmountOccurrence(val, m.start(), m.end()));
                }
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    private static class ExtractionContext {
        Double amount;
        String type; // JAMA | UDHAAR
        Double outstandingAmount;
    }

    private ExtractionContext extractContextFromClauses(String normalized, List<Double> amounts) {
        ExtractionContext ctx = new ExtractionContext();
        List<AmountOccurrence> occurrences = findAmountOccurrences(normalized);

        if (occurrences.isEmpty()) {
            return ctx;
        }

        if (occurrences.size() == 1) {
            double val = occurrences.get(0).value;
            String lower = normalized.toLowerCase();
            boolean isExplicitBaki = lower.contains("baki") || lower.contains("baaki") || lower.contains("bakaya") || lower.contains("due") || lower.contains("outstanding") || lower.contains("बाकी") || lower.contains("बकाया");
            boolean isJama = JAMA_KEYWORDS.stream().anyMatch(lower::contains);
            boolean isUdhaar = UDHAAR_KEYWORDS.stream().anyMatch(lower::contains);

            if (isExplicitBaki && !isJama) {
                ctx.outstandingAmount = val;
                ctx.amount = val;
                ctx.type = "UDHAAR";
            } else if (isJama) {
                ctx.amount = val;
                ctx.type = "JAMA";
            } else if (isUdhaar) {
                ctx.amount = val;
                ctx.type = "UDHAAR";
            } else {
                ctx.amount = val;
                ctx.type = classifyType(lower);
            }
            return ctx;
        }

        // Multiple distinct amounts detected in transcript
        for (int i = 0; i < occurrences.size(); i++) {
            AmountOccurrence occ = occurrences.get(i);
            int startBoundary = (i == 0) ? 0 : occurrences.get(i - 1).endIndex;
            int endBoundary = (i == occurrences.size() - 1) ? normalized.length() : occurrences.get(i + 1).startIndex;

            String segment = normalized.substring(startBoundary, endBoundary).toLowerCase();

            boolean hasOutstanding = OUTSTANDING_KEYWORDS.stream().anyMatch(segment::contains);
            boolean hasJama = JAMA_KEYWORDS.stream().anyMatch(segment::contains);
            boolean hasUdhaar = UDHAAR_KEYWORDS.stream().anyMatch(segment::contains);

            if (hasOutstanding && (ctx.amount != null || !hasJama)) {
                if (ctx.outstandingAmount == null) {
                    ctx.outstandingAmount = occ.value;
                }
            } else if (hasJama) {
                if (ctx.amount == null) {
                    ctx.amount = occ.value;
                    ctx.type = "JAMA";
                }
            } else if (hasUdhaar) {
                if (ctx.amount == null) {
                    ctx.amount = occ.value;
                    ctx.type = "UDHAAR";
                }
            } else {
                if (ctx.amount == null) {
                    ctx.amount = occ.value;
                } else if (ctx.outstandingAmount == null && (ctx.amount == null || occ.value != ctx.amount)) {
                    ctx.outstandingAmount = occ.value;
                }
            }
        }

        if (ctx.type == null) {
            ctx.type = classifyType(normalized.toLowerCase());
            if (ctx.type == null) ctx.type = "UDHAAR";
        }

        return ctx;
    }

    private String classifyType(String lower) {
        if (lower.contains("dene") || lower.contains("देने")) return "JAMA";
        if (lower.contains("lene") || lower.contains("लेने")) return "UDHAAR";

        boolean isJama = JAMA_KEYWORDS.stream().anyMatch(lower::contains);
        boolean isUdhaar = UDHAAR_KEYWORDS.stream().anyMatch(lower::contains);

        if (isJama && !isUdhaar) return "JAMA";
        if (isUdhaar && !isJama) return "UDHAAR";
        if (isJama) return "JAMA";
        if (isUdhaar) return "UDHAAR";
        return null;
    }

    public static String extractRawDevanagariName(String transcript) {
        if (transcript == null) return null;
        Matcher dm = DEVANAGARI_NAME_PATTERN.matcher(transcript);
        if (dm.find()) {
            return dm.group(1);
        }
        return null;
    }

    private String extractName(String transcript) {
        if (transcript == null) return null;
        // Latin name match
        Matcher lm = LATIN_NAME_PATTERN.matcher(transcript);
        if (lm.find()) {
            String candidate = lm.group(1);
            return Character.toUpperCase(candidate.charAt(0)) + candidate.substring(1).toLowerCase();
        }

        // Devanagari name match
        Matcher dm = DEVANAGARI_NAME_PATTERN.matcher(transcript);
        if (dm.find()) {
            String devName = dm.group(1);
            if (HINDI_NAME_MAP.containsKey(devName)) {
                return HINDI_NAME_MAP.get(devName);
            }
            return transliterateDevanagariName(devName);
        }

        // Fallback to first capitalized word
        for (String word : transcript.split("\\s+")) {
            String clean = word.replaceAll("[^A-Za-z]", "");
            if (clean.length() > 1 && Character.isUpperCase(clean.charAt(0))) {
                return clean;
            }
        }
        return null;
    }

    private String generateEnglishDescription(String transcript, String customerName, double amount, String type, Double outstandingAmount) {
        String lower = transcript.toLowerCase();
        String formattedAmount = String.format("%.0f", amount);

        // Standard patterns
        String baseDesc;
        if (lower.contains("lene") || lower.contains("लेने") || lower.contains("manga") || lower.contains("मांगा")) {
            baseDesc = "I have to collect " + formattedAmount + " rupees from " + customerName + ".";
        } else if (lower.contains("dene") || lower.contains("देने")) {
            baseDesc = "I have to pay " + customerName + " " + formattedAmount + " rupees.";
        } else if (lower.contains("liya") || lower.contains("mila") || lower.contains("received") ||
                   lower.contains("लिए") || lower.contains("मिला") || lower.contains("लिया")) {
            baseDesc = "I received " + formattedAmount + " rupees from " + customerName + ".";
        } else if ("UDHAAR".equals(type)) {
            baseDesc = "Credit of " + formattedAmount + " rupees given to " + customerName + ".";
        } else {
            baseDesc = "Payment of " + formattedAmount + " rupees received from " + customerName + ".";
        }

        if (outstandingAmount != null && !outstandingAmount.equals(amount)) {
            return baseDesc.substring(0, baseDesc.length() - 1) + " (Outstanding balance: " + String.format("%.0f", outstandingAmount) + " rupees).";
        }

        return baseDesc;
    }

    private String transliterateDevanagariName(String devName) {
        if (HINDI_NAME_MAP.containsKey(devName)) return HINDI_NAME_MAP.get(devName);
        return com.credlink.common.DevanagariTransliterator.transliterate(devName);
    }
}

