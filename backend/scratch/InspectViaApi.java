import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class InspectViaApi {

    private static final String BASE_URL = "http://localhost:8080/api/v1";

    public static void main(String[] args) throws Exception {
        // Request OTP for test merchant or ledger_verify
        String email = "ledger_verify@merchant.com";
        String reqJson = "{\"email\":\"" + email + "\",\"storeName\":\"Test Store\"}";
        sendPost(BASE_URL + "/auth/request-otp", reqJson, null);

        String json = "{\"email\":\"" + email + "\",\"otp\":\"123456\"}";
        String response = sendPost(BASE_URL + "/auth/verify-otp", json, null);
        String jwtToken = extractJsonField(response, "accessToken");

        System.out.println("=== LISTING ALL CUSTOMERS FOR " + email + " ===");
        String customersRes = sendGet(BASE_URL + "/customers", jwtToken);
        System.out.println(customersRes);

        System.out.println("\n=== TESTING NLP EXTRACTION FOR OUTSTANDING QUERY PHRASES ===");
        String[] phrases = {
            "Shivansh ka udhar kitna hai?",
            "Shivansh ka baki kitna hai?",
            "Shivansh ka bakaya kitna hai?",
            "Shivansh ka outstanding kitna hai?",
            "Shivansh ka due kitna hai?",
            "शिवांश का उधार कितना है?",
            "शिवांश का बकाया कितना है?"
        };

        for (String phrase : phrases) {
            String nlpJson = "{\"transcript\":\"" + phrase + "\"}";
            String nlpRes = sendPost(BASE_URL + "/nlp/extract", nlpJson, jwtToken);
            System.out.println("Phrase: '" + phrase + "'\n  Response: " + nlpRes);
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
