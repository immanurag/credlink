package com.credlink.customer;

import com.credlink.common.DevanagariTransliterator;
import com.credlink.common.exception.ApiException;
import com.credlink.customer.dto.CustomerRequest;
import com.credlink.customer.dto.CustomerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final Set<String> STOP_WORDS = Set.of(
            // Hinglish / English fillers and command verbs
            "se", "ko", "ka", "ki", "ne", "me", "mein", "par", "ke", "liye",
            "find", "search", "show", "check", "get", "tell", "batao", "dikhao", "khojo", "dhoondo", "dhundho",
            "balance", "dues", "udhaar", "udhar", "jama", "history", "statement", "transaction", "transactions", "account", "khata", "details",
            "kitna", "kitne", "kitni", "paise", "rupaye", "rs", "rupees", "hai", "hain", "ho", "karo", "karna", "dena", "lene", "dene",
            // Devanagari fillers and command verbs
            "से", "को", "का", "की", "ने", "में", "पर", "के", "लिए",
            "खोजो", "ढूंढो", "ढूंढें", "बताओ", "दिखाओ", "चेक",
            "बैलेंस", "हिस्ट्री", "खाता", "उधार", "जमा", "लेनदेन", "डिटेल्स",
            "कितना", "कितने", "कितनी", "पैसे", "रुपये", "है", "हैं", "हो", "करो", "करना", "देना", "लेने", "देने"
    );

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(Long merchantId, CustomerRequest req) {
        Customer c = new Customer();
        c.setMerchantId(merchantId);
        apply(c, req);
        return CustomerResponse.from(customerRepository.save(c));
    }

    public List<Customer> listAllForDebug() {
        return customerRepository.findAll();
    }

    public List<CustomerResponse> list(Long merchantId, String search) {
        List<Customer> allCustomers = customerRepository.findByMerchantId(merchantId);
        if (search == null || search.isBlank()) {
            return allCustomers.stream().map(CustomerResponse::from).toList();
        }
        if (allCustomers.isEmpty()) {
            return Collections.emptyList();
        }

        // Entity matching against existing merchant customers
        List<Customer> matched = matchCustomers(allCustomers, search);

        // Fallback to standard DB substring search if custom matching returned nothing
        if (matched.isEmpty()) {
            String cleaned = sanitizeSearchQuery(search);
            matched = customerRepository.findByMerchantIdAndNameContainingIgnoreCase(merchantId, cleaned);

            if (matched.isEmpty() && cleaned.contains(" ")) {
                String firstWord = cleaned.split("\\s+")[0];
                if (!firstWord.isBlank() && firstWord.length() >= 2) {
                    matched = customerRepository.findByMerchantIdAndNameContainingIgnoreCase(merchantId, firstWord);
                }
            }
        }

        return matched.stream().map(CustomerResponse::from).toList();
    }

    private List<Customer> matchCustomers(List<Customer> customers, String searchQuery) {
        String cleanRaw = searchQuery.replaceAll("[.,?!'\"]", " ").trim();
        String[] words = cleanRaw.split("\\s+");

        List<String> candidateWords = new ArrayList<>();
        for (String w : words) {
            String lower = w.toLowerCase();
            if (!STOP_WORDS.contains(lower) && !w.isBlank()) {
                candidateWords.add(w);
            }
        }

        // If all words were stop-words, fallback to using all words
        if (candidateWords.isEmpty()) {
            for (String w : words) {
                if (!w.isBlank()) candidateWords.add(w);
            }
        }

        Map<Customer, Integer> scores = new HashMap<>();

        for (Customer c : customers) {
            String cName = c.getName();
            if (cName == null) continue;
            String normCName = DevanagariTransliterator.normalizePhonetic(cName);
            String cFirstName = cName.split("\\s+")[0];
            String normCFirstName = DevanagariTransliterator.normalizePhonetic(cFirstName);
            String cPhone = c.getPhone();

            int maxScore = 0;

            // Check phone match
            if (cPhone != null && !cPhone.isBlank()) {
                String digitsOnly = searchQuery.replaceAll("[^0-9]", "");
                if (!digitsOnly.isEmpty() && cPhone.contains(digitsOnly)) {
                    maxScore = Math.max(maxScore, 100);
                }
            }

            for (String candidate : candidateWords) {
                String romanCandidate = DevanagariTransliterator.transliterate(candidate);
                String normCandidate = DevanagariTransliterator.normalizePhonetic(candidate);

                // Exact full name match (Roman or Phonetic)
                if (cName.equalsIgnoreCase(candidate) || cName.equalsIgnoreCase(romanCandidate) || normCName.equalsIgnoreCase(normCandidate)) {
                    maxScore = Math.max(maxScore, 100);
                }
                // First name exact match
                else if (cFirstName.equalsIgnoreCase(candidate) || cFirstName.equalsIgnoreCase(romanCandidate) || normCFirstName.equalsIgnoreCase(normCandidate)) {
                    maxScore = Math.max(maxScore, 95);
                }
                // Substring match
                else if (cName.toLowerCase().contains(romanCandidate.toLowerCase()) || romanCandidate.toLowerCase().contains(cName.toLowerCase())
                         || normCName.contains(normCandidate) || normCandidate.contains(normCName)) {
                    maxScore = Math.max(maxScore, 85);
                }
                // Phonetic variant Levenshtein match
                else if (normCName.length() >= 3 && normCandidate.length() >= 3 && computeLevenshteinDistance(normCName, normCandidate) <= 2) {
                    maxScore = Math.max(maxScore, 75);
                }
                else if (normCFirstName.length() >= 3 && normCandidate.length() >= 3 && computeLevenshteinDistance(normCFirstName, normCandidate) <= 1) {
                    maxScore = Math.max(maxScore, 70);
                }
            }

            if (maxScore > 0) {
                scores.put(c, maxScore);
            }
        }

        return scores.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int computeLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    public CustomerResponse get(Long merchantId, Long customerId) {
        return CustomerResponse.from(findOwned(merchantId, customerId));
    }

    @Transactional
    public CustomerResponse update(Long merchantId, Long customerId, CustomerRequest req) {
        Customer c = findOwned(merchantId, customerId);
        apply(c, req);
        return CustomerResponse.from(customerRepository.save(c));
    }

    @Transactional
    public void delete(Long merchantId, Long customerId) {
        Customer c = findOwned(merchantId, customerId);
        customerRepository.delete(c);
    }

    Customer findOwned(Long merchantId, Long customerId) {
        return customerRepository.findByIdAndMerchantId(customerId, merchantId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer was not found."));
    }

    private String sanitizeSearchQuery(String query) {
        if (query == null) return "";
        String cleaned = query.replaceAll("[.,?!'\"]", "").trim();
        cleaned = cleaned.replaceAll("(?i)\\s+(?:se|ko|ka|ki|ne|से|को|का|की|ने)$", "").trim();
        return cleaned;
    }

    private void apply(Customer c, CustomerRequest req) {
        c.setName(req.getName());
        c.setPhone(req.getPhone());
        c.setAddress(req.getAddress());
        c.setCategory(req.getCategory());
        c.setCreditLimit(req.getCreditLimit());
    }
}
