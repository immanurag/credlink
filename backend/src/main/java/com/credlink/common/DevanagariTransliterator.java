package com.credlink.common;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class DevanagariTransliterator {

    private static final Pattern DEVANAGARI_PATTERN = Pattern.compile("[\u0900-\u097F]");

    private static final Map<Character, String> VOWELS = new HashMap<>();
    private static final Map<Character, String> CONSONANTS = new HashMap<>();
    private static final Map<Character, String> MATRAS = new HashMap<>();

    static {
        // Independent Vowels
        VOWELS.put('\u0905', "a");  // अ
        VOWELS.put('\u0906', "a");  // आ
        VOWELS.put('\u0907', "i");  // इ
        VOWELS.put('\u0908', "i");  // ई
        VOWELS.put('\u0909', "u");  // उ
        VOWELS.put('\u090A', "u");  // ऊ
        VOWELS.put('\u090B', "ri"); // ऋ
        VOWELS.put('\u090F', "e");  // ए
        VOWELS.put('\u0910', "ai"); // ऐ
        VOWELS.put('\u0913', "o");  // ओ
        VOWELS.put('\u0914', "au"); // औ

        // Consonants
        CONSONANTS.put('\u0915', "k");  // क
        CONSONANTS.put('\u0916', "kh"); // ख
        CONSONANTS.put('\u0917', "g");  // ग
        CONSONANTS.put('\u0918', "gh"); // घ
        CONSONANTS.put('\u0919', "ng"); // ङ
        CONSONANTS.put('\u091A', "ch"); // च
        CONSONANTS.put('\u091B', "chh");// छ
        CONSONANTS.put('\u091C', "j");  // ज
        CONSONANTS.put('\u091D', "jh"); // झ
        CONSONANTS.put('\u091E', "ny"); // ञ
        CONSONANTS.put('\u091F', "t");  // ट
        CONSONANTS.put('\u0920', "th"); // ठ
        CONSONANTS.put('\u0921', "d");  // ड
        CONSONANTS.put('\u0922', "dh"); // ढ
        CONSONANTS.put('\u0923', "n");  // ण
        CONSONANTS.put('\u0924', "t");  // त
        CONSONANTS.put('\u0925', "th"); // थ
        CONSONANTS.put('\u0926', "d");  // द
        CONSONANTS.put('\u0927', "dh"); // ध
        CONSONANTS.put('\u0928', "n");  // न
        CONSONANTS.put('\u092A', "p");  // प
        CONSONANTS.put('\u092B', "ph"); // फ
        CONSONANTS.put('\u092C', "b");  // ब
        CONSONANTS.put('\u092D', "bh"); // भ
        CONSONANTS.put('\u092E', "m");  // म
        CONSONANTS.put('\u092F', "y");  // य
        CONSONANTS.put('\u0930', "r");  // र
        CONSONANTS.put('\u0932', "l");  // ल
        CONSONANTS.put('\u0933', "l");  // ळ
        CONSONANTS.put('\u0935', "v");  // व
        CONSONANTS.put('\u0936', "sh"); // श
        CONSONANTS.put('\u0937', "sh"); // ष
        CONSONANTS.put('\u0938', "s");  // स
        CONSONANTS.put('\u0939', "h");  // ह

        // Nukta variants
        CONSONANTS.put('\u0958', "q");  // क़
        CONSONANTS.put('\u0959', "kh"); // ख़
        CONSONANTS.put('\u095A', "g");  // ग़
        CONSONANTS.put('\u095B', "z");  // ज़
        CONSONANTS.put('\u095C', "r");  // ड़
        CONSONANTS.put('\u095D', "rh"); // ढ़
        CONSONANTS.put('\u095E', "f");  // फ़

        // Vowel signs (Matras)
        MATRAS.put('\u093E', "a");  // ा
        MATRAS.put('\u093F', "i");  // ि
        MATRAS.put('\u0940', "i");  // ी
        MATRAS.put('\u0941', "u");  // ु
        MATRAS.put('\u0942', "u");  // ू
        MATRAS.put('\u0943', "ri"); // ृ
        MATRAS.put('\u0947', "e");  // े
        MATRAS.put('\u0948', "ai"); // ै
        MATRAS.put('\u094B', "o");  // ो
        MATRAS.put('\u094C', "au"); // ौ
    }

    public static boolean containsDevanagari(String text) {
        return text != null && DEVANAGARI_PATTERN.matcher(text).find();
    }

    public static String transliterate(String text) {
        if (text == null || text.isBlank()) return "";
        if (!containsDevanagari(text)) return text;

        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+");

        for (int w = 0; w < words.length; w++) {
            if (w > 0) result.append(" ");
            result.append(transliterateWord(words[w]));
        }

        return result.toString();
    }

    private static String transliterateWord(String word) {
        if (!containsDevanagari(word)) return word;

        StringBuilder sb = new StringBuilder();
        int len = word.length();

        for (int i = 0; i < len; i++) {
            char c = word.charAt(i);

            // 1. Independent Vowels
            if (VOWELS.containsKey(c)) {
                sb.append(VOWELS.get(c));
                continue;
            }

            // 2. Consonants
            if (CONSONANTS.containsKey(c)) {
                String baseConsonant = CONSONANTS.get(c);
                sb.append(baseConsonant);

                char next = (i + 1 < len) ? word.charAt(i + 1) : '\0';

                // Check modifier after consonant
                if (next == '\u094D') {
                    // Halant / Virama: kills inherent vowel
                    i++; // skip halant
                } else if (MATRAS.containsKey(next)) {
                    // Matra: replaces inherent vowel
                    sb.append(MATRAS.get(next));
                    i++; // skip matra
                } else if (next == '\u0902' || next == '\u0901') {
                    // Anusvara / Anunasika (ं, ँ)
                    sb.append("an");
                    i++; // skip anusvara
                } else {
                    // Inherent vowel 'a'
                    // Hindi Schwa deletion rule:
                    // Drop terminal 'a' at end of word unless it's a single letter or preceded by halant
                    boolean isLastCharInWord = (i + 1 >= len);
                    if (!isLastCharInWord) {
                        sb.append("a");
                    }
                }
                continue;
            }

            // Standalone Anusvara / Visarga if not caught after consonant
            if (c == '\u0902' || c == '\u0901') {
                sb.append("n");
            } else if (c == '\u0903') {
                sb.append("h");
            } else if (!MATRAS.containsKey(c) && c != '\u094D') {
                // Non-Devanagari char (e.g. latin letter or digit)
                sb.append(c);
            }
        }

        String res = sb.toString().trim();
        if (!res.isEmpty()) {
            return Character.toUpperCase(res.charAt(0)) + res.substring(1);
        }
        return res;
    }

    /**
     * Normalizes a string for phonetic / variant comparison:
     * - Converts to lowercase
     * - Replaces common spelling variations (ee -> i, oo -> u, sh -> s, v -> w, etc.)
     * - Removes punctuation/whitespace
     */
    public static String normalizePhonetic(String input) {
        if (input == null) return "";
        String s = transliterate(input).toLowerCase();
        s = s.replaceAll("[^a-z0-9]", "");
        s = s.replace("ee", "i")
             .replace("oo", "u")
             .replace("sh", "s")
             .replace("v", "w")
             .replace("ph", "f")
             .replace("ck", "k");
        return s;
    }
}
