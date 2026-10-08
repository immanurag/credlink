import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestLedgerFlow {

    private static String jwtToken;
    private static final String BASE_URL = "http://localhost:8080/api/v1";

    public static void main(String[] args) throws Exception {
        System.out.println("=== STARTING LEDGER FLOW TEST ===");

        // 1. Login to get JWT Token
        login();

        // 2. Create customer Shivansh
        long customerId = createCustomer("Shivansh", "9812345678");
        System.out.println("Created/Retrieved Customer 'Shivansh' with ID: " + customerId);

        // Verify starting balance is 0.0
        verifyAllSources(customerId, "Shivansh", 0.0, "Initial State");

        // Transaction 1: UDHAAR ₹1000 -> Expected Due: ₹1000
        createAndConfirmTransaction(customerId, "UDHAAR", 1000.0, "Credit 1000");
        verifyAllSources(customerId, "Shivansh", 1000.0, "Transaction 1 (UDHAAR 1000)");

        // Transaction 2: JAMA ₹300 -> Expected Due: ₹700
        createAndConfirmTransaction(customerId, "JAMA", 300.0, "Payment 300");
        verifyAllSources(customerId, "Shivansh", 700.0, "Transaction 2 (JAMA 300)");

        // Transaction 3: UDHAAR ₹500 -> Expected Due: ₹1200
        createAndConfirmTransaction(customerId, "UDHAAR", 500.0, "Credit 500");
        verifyAllSources(customerId, "Shivansh", 1200.0, "Transaction 3 (UDHAAR 500)");

        // Transaction 4: JAMA ₹200 -> Expected Due: ₹1000
        createAndConfirmTransaction(customerId, "JAMA", 200.0, "Payment 200");
        verifyAllSources(customerId, "Shivansh", 1000.0, "Transaction 4 (JAMA 200)");

        System.out.println("\n=== ALL 4 STEPS PASSED SUCCESSFULLY ===");
    }

    private static void login() throws Exception {
        String email = "ledger_verify@merchant.com";
        String reqJson = "{\"email\":\"" + email + "\",\"storeName\":\"Test Store\"}";
        sendPost(BASE_URL + "/auth/request-otp", reqJson);

        String json = "{\"email\":\"" + email + "\",\"otp\":\"123456\"}";
        String response = sendPost(BASE_URL + "/auth/verify-otp", json);
        jwtToken = extractJsonField(response, "accessToken");
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new RuntimeException("Failed to obtain JWT token. Response: " + response);
        }
        System.out.println("Logged in successfully as " + email + ".");
    }

    private static long createCustomer(String name, String phone) throws Exception {
        // First check if customer already exists
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
        // Preview
        String previewJson = "{\"customerId\":" + customerId + ",\"type\":\"" + type + "\",\"amount\":" + amount + ",\"description\":\"" + desc + "\"}";
        String previewResponse = sendPost(BASE_URL + "/transactions/preview", previewJson);
        String txIdStr = extractJsonField(previewResponse, "id");
        long txId = Long.parseLong(txIdStr);

        // Confirm
        String confirmJson = "{\"type\":\"" + type + "\",\"amount\":" + amount + "}";
        sendPost(BASE_URL + "/transactions/" + txId + "/confirm", confirmJson);
    }

    private static void verifyAllSources(long customerId, String customerName, double expectedBalance, String stage) throws Exception {
        System.out.println("\n--- Verifying " + stage + " [Expected Due: ₹" + expectedBalance + "] ---");

        // 1. Customer Details API
        String customerRes = sendGet(BASE_URL + "/customers/" + customerId);
        double custBalance = Double.parseDouble(extractJsonField(customerRes, "currentBalance"));
        System.out.println("1. Customer Details Balance: ₹" + custBalance);
        if (Math.abs(custBalance - expectedBalance) > 0.01) {
            throw new RuntimeException("Mismatch in Customer Details! Expected: " + expectedBalance + ", Got: " + custBalance);
        }

        // 2. Outstanding API
        String balanceRes = sendGet(BASE_URL + "/customers/" + customerId + "/balance");
        double apiBalance = Double.parseDouble(extractJsonField(balanceRes, "balance"));
        System.out.println("2. Outstanding API Balance: ₹" + apiBalance);
        if (Math.abs(apiBalance - expectedBalance) > 0.01) {
            throw new RuntimeException("Mismatch in Outstanding API! Expected: " + expectedBalance + ", Got: " + apiBalance);
        }

        // 3. Account Statement
        String statementRes = sendGet(BASE_URL + "/reports/customers/" + customerId + "/statement");
        double statementClosing = Double.parseDouble(extractJsonField(statementRes, "closingBalance"));
        System.out.println("3. Account Statement Closing Balance: ₹" + statementClosing);
        if (Math.abs(statementClosing - expectedBalance) > 0.01) {
            throw new RuntimeException("Mismatch in Account Statement! Expected: " + expectedBalance + ", Got: " + statementClosing);
        }

        // 4. Voice Outstanding Query ("Shivansh ka bakaya kitna hai?")
        String nlpJson = "{\"transcript\":\"" + customerName + " ka bakaya kitna hai?\"}";
        String nlpRes = sendPost(BASE_URL + "/nlp/extract", nlpJson);
        double nlpBalance = Double.parseDouble(extractJsonField(nlpRes, "outstandingAmount"));
        String responseMessage = extractJsonField(nlpRes, "responseMessage");
        System.out.println("4. Voice Query Outstanding: ₹" + nlpBalance + " (Message: \"" + responseMessage + "\")");
        if (Math.abs(nlpBalance - expectedBalance) > 0.01) {
            throw new RuntimeException("Mismatch in Voice Query! Expected: " + expectedBalance + ", Got: " + nlpBalance);
        }
    }

    private static String sendGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }
        return readResponse(conn);
    }

    private static String sendPost(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (jwtToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }
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
