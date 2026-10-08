import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestRestartPersistence {

    private static String jwtToken;
    private static final String BASE_URL = "http://localhost:8080/api/v1";

    public static void main(String[] args) throws Exception {
        System.out.println("=== STARTING RESTART PERSISTENCE VERIFICATION ===");

        // 1. Search for existing Shivansh customer across existing merchant accounts or create & lookup
        // We will login using request-otp & verify-otp to get JWT token
        login();

        // Search for Shivansh
        String listResponse = sendGet(BASE_URL + "/customers?search=Shivansh");
        if (!listResponse.contains("\"id\":")) {
            System.out.println("No customer Shivansh found in this merchant session, searching all...");
            throw new RuntimeException("Shivansh customer not found after restart.");
        }

        String idStr = extractJsonField(listResponse, "id");
        long customerId = Long.parseLong(idStr);
        System.out.println("Found customer 'Shivansh' after server restart with ID: " + customerId);

        double expectedBalance = 1000.0;

        // 1. Customer Details API
        String customerRes = sendGet(BASE_URL + "/customers/" + customerId);
        double custBalance = Double.parseDouble(extractJsonField(customerRes, "currentBalance"));
        System.out.println("1. Customer Details Balance: ₹" + custBalance);

        // 2. Outstanding API
        String balanceRes = sendGet(BASE_URL + "/customers/" + customerId + "/balance");
        double apiBalance = Double.parseDouble(extractJsonField(balanceRes, "balance"));
        System.out.println("2. Outstanding API Balance: ₹" + apiBalance);

        // 3. Account Statement
        String statementRes = sendGet(BASE_URL + "/reports/customers/" + customerId + "/statement");
        double statementClosing = Double.parseDouble(extractJsonField(statementRes, "closingBalance"));
        System.out.println("3. Account Statement Closing Balance: ₹" + statementClosing);

        // 4. Voice Outstanding Query ("Shivansh ka bakaya kitna hai?")
        String nlpJson = "{\"transcript\":\"Shivansh ka bakaya kitna hai?\"}";
        String nlpRes = sendPost(BASE_URL + "/nlp/extract", nlpJson);
        double nlpBalance = Double.parseDouble(extractJsonField(nlpRes, "outstandingAmount"));
        String responseMessage = extractJsonField(nlpRes, "responseMessage");
        System.out.println("4. Voice Query Outstanding: ₹" + nlpBalance + " (Message: \"" + responseMessage + "\")");

        if (Math.abs(custBalance - expectedBalance) < 0.01 &&
            Math.abs(apiBalance - expectedBalance) < 0.01 &&
            Math.abs(statementClosing - expectedBalance) < 0.01 &&
            Math.abs(nlpBalance - expectedBalance) < 0.01) {
            System.out.println("\n=== RESTART PERSISTENCE VERIFICATION PASSED: ALL 4 SOURCES MATCH ₹1000.0 ===");
        } else {
            throw new RuntimeException("Restart persistence verification failed! Balance mismatch.");
        }
    }

    private static void login() throws Exception {
        String email = "ledger_verify@merchant.com";
        String reqJson = "{\"email\":\"" + email + "\",\"storeName\":\"Test Store\"}";
        sendPost(BASE_URL + "/auth/request-otp", reqJson);

        String json = "{\"email\":\"" + email + "\",\"otp\":\"123456\"}";
        String response = sendPost(BASE_URL + "/auth/verify-otp", json);
        jwtToken = extractJsonField(response, "accessToken");
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
