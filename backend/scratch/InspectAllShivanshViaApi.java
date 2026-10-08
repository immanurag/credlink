import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class InspectAllShivanshViaApi {

    private static final String BASE_URL = "http://localhost:8080/api/v1";

    public static void main(String[] args) throws Exception {
        // Log in using merchant credentials
        String email = "test@merchant.com";
        String reqJson = "{\"email\":\"" + email + "\",\"storeName\":\"Test Store\"}";
        try { sendPost(BASE_URL + "/auth/request-otp", reqJson, null); } catch (Exception ignored) {}

        String json = "{\"email\":\"" + email + "\",\"otp\":\"123456\"}";
        String response = sendPost(BASE_URL + "/auth/verify-otp", json, null);
        String jwtToken = extractJsonField(response, "accessToken");

        System.out.println("=== LISTING ALL CUSTOMERS ACROSS ALL MERCHANTS ===");
        String customersRes = sendGet(BASE_URL + "/customers/debug/all", jwtToken);
        System.out.println(customersRes);

        // Find customer Shivansh ID
        String idStr = extractJsonField(customersRes, "id");
        if (idStr != null) {
            long customerId = Long.parseLong(idStr);
            System.out.println("\n=== CUSTOMER DETAILS FOR ID " + customerId + " ===");
            String custDetails = sendGet(BASE_URL + "/customers/" + customerId, jwtToken);
            System.out.println(custDetails);

            System.out.println("\n=== TRANSACTIONS FOR CUSTOMER ID " + customerId + " ===");
            String txRes = sendGet(BASE_URL + "/customers/" + customerId + "/transactions", jwtToken);
            System.out.println(txRes);

            System.out.println("\n=== STATEMENT FOR CUSTOMER ID " + customerId + " ===");
            String stmtRes = sendGet(BASE_URL + "/reports/customers/" + customerId + "/statement", jwtToken);
            System.out.println(stmtRes);
        }
    }

    private static String sendGet(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
        return readResponse(conn);
    }

    private static String sendPost(String urlStr, String jsonBody, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
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
