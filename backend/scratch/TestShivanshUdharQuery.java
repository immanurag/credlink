import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestShivanshUdharQuery {

    private static String jwtToken;
    private static final String BASE_URL = "http://localhost:8080/api/v1";

    public static void main(String[] args) throws Exception {
        System.out.println("=== STARTING SHIVANSH UDHAR QUERY VERIFICATION ===");

        login();

        // Create or get customer Shivansh
        long customerId = createCustomer("Shivansh", "9988776655");
        System.out.println("Customer Shivansh ID: " + customerId);

        // Fetch current customer balance
        String custRes = sendGet(BASE_URL + "/customers/" + customerId);
        double startingBalance = Double.parseDouble(extractJsonField(custRes, "currentBalance"));
        System.out.println("Starting Balance of Shivansh: ₹" + startingBalance);

        // We want Shivansh to have current outstanding = ₹1100.
        // Let's create transactions:
        // UDHAAR 5000
        // JAMA 3900
        // UDHAAR 2000
        // JAMA 2000
        // Total Udhaar = 7000, Total Jama = 5900 -> Net Due = 1100
        // If startingBalance is not 0, let's reset or adjust so net due is 1100.0.
        if (Math.abs(startingBalance - 1100.0) > 0.01) {
            // Add transactions to make current due exactly 1100
            if (startingBalance > 0) {
                createAndConfirmTransaction(customerId, "JAMA", startingBalance, "Adjust balance to zero");
            } else if (startingBalance < 0) {
                createAndConfirmTransaction(customerId, "UDHAAR", Math.abs(startingBalance), "Adjust balance to zero");
            }

            // Now create the exact transaction set:
            createAndConfirmTransaction(customerId, "UDHAAR", 5000.0, "Udhaar 5000");
            createAndConfirmTransaction(customerId, "JAMA", 3900.0, "Jama 3900");
            createAndConfirmTransaction(customerId, "UDHAAR", 2000.0, "Udhaar 2000");
            createAndConfirmTransaction(customerId, "JAMA", 2000.0, "Jama 2000");
        }

        // Fetch metrics to verify:
        String updatedCustRes = sendGet(BASE_URL + "/customers/" + customerId);
        double currentDue = Double.parseDouble(extractJsonField(updatedCustRes, "currentBalance"));

        String statementRes = sendGet(BASE_URL + "/reports/customers/" + customerId + "/statement");
        double totalUdhaar = Double.parseDouble(extractJsonField(statementRes, "totalUdhaar"));
        double totalJama = Double.parseDouble(extractJsonField(statementRes, "totalJama"));
        double closingBalance = Double.parseDouble(extractJsonField(statementRes, "closingBalance"));

        double totalTransactionValue = totalUdhaar + totalJama;

        System.out.println("\n--- SHIVANSH LEDGER SUMMARY ---");
        System.out.println("Customer: Shivansh");
        System.out.println("Total Udhaar: ₹" + totalUdhaar);
        System.out.println("Total Jama: ₹" + totalJama);
        System.out.println("Total Transaction Value: ₹" + totalTransactionValue);
        System.out.println("Current Outstanding/Due: ₹" + currentDue);

        if (Math.abs(currentDue - 1100.0) > 0.01) {
            throw new RuntimeException("Setup failed: Current due is not 1100.0, got: " + currentDue);
        }

        // Test all required voice query variations
        String[] queryPhrases = {
            "Shivansh ka udhar kitna hai?",
            "Shivansh ka baki kitna hai?",
            "Shivansh ka bakaya kitna hai?",
            "Shivansh ka outstanding kitna hai?",
            "Shivansh ka due kitna hai?",
            "Shivansh ke kitne paise baki hain?",
            "Shivansh se kitna lena hai?",
            "शिवांश का उधार कितना है?",
            "शिवांश का बकाया कितना है?"
        };

        System.out.println("\n=== TESTING VOICE QUERIES FOR SHIVANSH ===");
        for (String phrase : queryPhrases) {
            String nlpJson = "{\"transcript\":\"" + phrase + "\"}";
            String nlpRes = sendPost(BASE_URL + "/nlp/extract", nlpJson);
            System.out.println("nlpRes for '" + phrase + "': " + nlpRes);

            String intent = extractJsonField(nlpRes, "intent");
            double nlpOutstanding = Double.parseDouble(extractJsonField(nlpRes, "outstandingAmount"));
            String responseMsg = extractJsonField(nlpRes, "responseMessage");

            System.out.println("Query: '" + phrase + "'");
            System.out.println("  -> Intent: " + intent);
            System.out.println("  -> Outstanding Amount Returned: ₹" + nlpOutstanding);
            System.out.println("  -> Final Response Message: \"" + responseMsg + "\"");

            if (!"OUTSTANDING_BALANCE_QUERY".equals(intent)) {
                throw new RuntimeException("Intent was not OUTSTANDING_BALANCE_QUERY for: " + phrase);
            }

            if (Math.abs(nlpOutstanding - 1100.0) > 0.01) {
                throw new RuntimeException("INCORRECT AMOUNT RETURNED! Expected ₹1100.0, got ₹" + nlpOutstanding);
            }
        }

        System.out.println("\n=== ALL TEST CASES PASSED SUCCESSFULLY: RETURNED ₹1100 FOR ALL PHRASES ===");
    }

    private static void login() throws Exception {
        String email = "shivansh_test_" + System.currentTimeMillis() + "@merchant.com";
        String reqJson = "{\"email\":\"" + email + "\",\"storeName\":\"Test Store\"}";
        sendPost(BASE_URL + "/auth/request-otp", reqJson);

        String json = "{\"email\":\"" + email + "\",\"otp\":\"123456\"}";
        String response = sendPost(BASE_URL + "/auth/verify-otp", json);
        jwtToken = extractJsonField(response, "accessToken");
    }

    private static long createCustomer(String name, String phone) throws Exception {
        String listResponse = sendGet(BASE_URL + "/customers?search=" + name);
        if (listResponse.contains("\"id\":")) {
            String idStr = extractJsonField(listResponse, "id");
            if (idStr != null) return Long.parseLong(idStr);
        }

        String json = "{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\"}";
        String response = sendPost(BASE_URL + "/customers", json);
        String idStr = extractJsonField(response, "id");
        return Long.parseLong(idStr);
    }

    private static void createAndConfirmTransaction(long customerId, String type, double amount, String desc) throws Exception {
        String previewJson = "{\"customerId\":" + customerId + ",\"type\":\"" + type + "\",\"amount\":" + amount + ",\"description\":\"" + desc + "\"}";
        String previewResponse = sendPost(BASE_URL + "/transactions/preview", previewJson);
        String txIdStr = extractJsonField(previewResponse, "id");
        long txId = Long.parseLong(txIdStr);

        String confirmJson = "{\"type\":\"" + type + "\",\"amount\":" + amount + "}";
        sendPost(BASE_URL + "/transactions/" + txId + "/confirm", confirmJson);
    }

    private static String sendGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        return readResponse(conn);
    }

    private static String sendPost(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } else {
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                end++;
            }
            return json.substring(start, end);
        }
    }
}
