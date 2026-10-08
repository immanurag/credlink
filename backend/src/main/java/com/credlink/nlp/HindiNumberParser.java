package com.credlink.nlp;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort parser for spoken Hindi/Hinglish amounts ("do sau", "paanch sau pachas", "पाँच सौ", "2000").
 * Converts Romanized or Devanagari Hindi number words and digits into standard numeric doubles.
 */
public final class HindiNumberParser {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");

    private static final Map<String, Integer> UNITS = new LinkedHashMap<>();
    private static final Map<String, Integer> MULTIPLIERS = new LinkedHashMap<>();

    static {
        // Romanized Hindi / English
        UNITS.put("ek", 1); UNITS.put("one", 1);
        UNITS.put("do", 2); UNITS.put("two", 2);
        UNITS.put("teen", 3); UNITS.put("three", 3);
        UNITS.put("char", 4); UNITS.put("chaar", 4); UNITS.put("four", 4);
        UNITS.put("paanch", 5); UNITS.put("panch", 5); UNITS.put("pach", 5); UNITS.put("five", 5);
        UNITS.put("chhe", 6); UNITS.put("che", 6); UNITS.put("six", 6);
        UNITS.put("saat", 7); UNITS.put("seven", 7);
        UNITS.put("aath", 8); UNITS.put("eight", 8);
        UNITS.put("nau", 9); UNITS.put("nine", 9);
        UNITS.put("das", 10); UNITS.put("dus", 10); UNITS.put("ten", 10);
        UNITS.put("gyarah", 11); UNITS.put("barah", 12); UNITS.put("terah", 13); UNITS.put("chaudah", 14);
        UNITS.put("pandrah", 15); UNITS.put("solah", 16); UNITS.put("satrah", 17); UNITS.put("atharah", 18);
        UNITS.put("unnis", 19); UNITS.put("bees", 20); UNITS.put("tees", 30); UNITS.put("chalis", 40);
        UNITS.put("pachas", 50); UNITS.put("saath", 60); UNITS.put("sattar", 70); UNITS.put("assi", 80);
        UNITS.put("nabbe", 90);

        // Devanagari Hindi words
        UNITS.put("एक", 1);
        UNITS.put("दो", 2);
        UNITS.put("तीन", 3);
        UNITS.put("चार", 4);
        UNITS.put("पांच", 5); UNITS.put("पाँच", 5);
        UNITS.put("छह", 6); UNITS.put("छः", 6);
        UNITS.put("सात", 7);
        UNITS.put("आठ", 8);
        UNITS.put("नौ", 9);
        UNITS.put("दस", 10);
        UNITS.put("ग्यारह", 11); UNITS.put("बारह", 12); UNITS.put("तेरह", 13); UNITS.put("चौदह", 14);
        UNITS.put("पंद्रह", 15); UNITS.put("सोलह", 16); UNITS.put("सत्रह", 17); UNITS.put("अठारह", 18);
        UNITS.put("उन्नीस", 19); UNITS.put("बीस", 20); UNITS.put("तीस", 30); UNITS.put("चालीस", 40);
        UNITS.put("पचास", 50); UNITS.put("साठ", 60); UNITS.put("सत्तर", 70); UNITS.put("अस्सी", 80);
        UNITS.put("नब्बे", 90);

        // Multipliers
        MULTIPLIERS.put("sau", 100); MULTIPLIERS.put("hundred", 100); MULTIPLIERS.put("सौ", 100);
        MULTIPLIERS.put("hazar", 1000); MULTIPLIERS.put("hazaar", 1000); MULTIPLIERS.put("thousand", 1000);
        MULTIPLIERS.put("हजार", 1000); MULTIPLIERS.put("हज़ार", 1000);
        MULTIPLIERS.put("lakh", 100000); MULTIPLIERS.put("lac", 100000); MULTIPLIERS.put("लाख", 100000);
    }

    private HindiNumberParser() {}

    /** Normalizes Devanagari digits to ASCII 0-9 */
    public static String normalizeDigits(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= '\u0966' && c <= '\u096F') {
                sb.append((char) ('0' + (c - '\u0966')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Converts Hindi/Hinglish word numbers (e.g., "पाँच सौ", "do sau", "pandrah sau") to digits in text */
    public static String normalizeWordNumbersToDigits(String transcript) {
        if (transcript == null || transcript.isBlank()) return transcript;
        String text = normalizeDigits(transcript);

        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String clean = word.replaceAll("[^a-zA-Z\\u0900-\\u097F0-9]", "").toLowerCase();

            Integer unitVal = parseSingleTokenNumber(clean);
            if (unitVal != null && i + 1 < words.length) {
                String nextClean = words[i + 1].replaceAll("[^a-zA-Z\\u0900-\\u097F0-9]", "").toLowerCase();
                if (MULTIPLIERS.containsKey(nextClean)) {
                    int mult = MULTIPLIERS.get(nextClean);
                    int val = unitVal * mult;

                    if (i + 2 < words.length) {
                        String subClean = words[i + 2].replaceAll("[^a-zA-Z\\u0900-\\u097F0-9]", "").toLowerCase();
                        Integer subUnit = parseSingleTokenNumber(subClean);
                        if (subUnit != null && subUnit < mult && !MULTIPLIERS.containsKey(subClean)) {
                            val += subUnit;
                            i++;
                        }
                    }

                    if (result.length() > 0) result.append(" ");
                    result.append(val);
                    String punct = words[i + 1].replaceAll("[a-zA-Z\\u0900-\\u097F0-9]", "");
                    if (!punct.isEmpty()) result.append(punct);
                    i++;
                    continue;
                }
            }

            if (UNITS.containsKey(clean) && UNITS.get(clean) >= 10 && !isLikelyName(clean)) {
                if (result.length() > 0) result.append(" ");
                result.append(UNITS.get(clean));
                String punct = word.replaceAll("[a-zA-Z\\u0900-\\u097F0-9]", "");
                if (!punct.isEmpty()) result.append(punct);
                continue;
            }

            if (result.length() > 0) result.append(" ");
            result.append(word);
        }

        return result.toString();
    }

    private static Integer parseSingleTokenNumber(String token) {
        if (token.matches("\\d+")) {
            return Integer.parseInt(token);
        }
        return UNITS.get(token);
    }

    private static boolean isLikelyName(String token) {
        return "do".equalsIgnoreCase(token) || "das".equalsIgnoreCase(token);
    }

    /** Returns the first amount-like value found in the transcript, or null if none is found. */
    public static Double extractAmount(String transcript) {
        List<Double> amounts = extractAmounts(transcript);
        return amounts.isEmpty() ? null : amounts.get(0);
    }

    /** Extracts all distinct numeric amounts from transcript without summing them. */
    public static List<Double> extractAmounts(String transcript) {
        if (transcript == null || transcript.isBlank()) return Collections.emptyList();
        String normalized = normalizeWordNumbersToDigits(transcript);

        List<Double> list = new ArrayList<>();
        Matcher m = DIGITS_PATTERN.matcher(normalized);
        while (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                if (val > 0) {
                    list.add(val);
                }
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }
}

