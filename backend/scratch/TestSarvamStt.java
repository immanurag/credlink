package com.credlink;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;

public class TestSarvamStt {
    public static void main(String[] args) {
        String apiKey = System.getenv("SARVAM_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("SARVAM_API_KEY not found in environment.");
            return;
        }

        System.out.println("Testing Sarvam API Key: " + apiKey.substring(0, Math.min(8, apiKey.length())) + "...");

        // Check if there are any test audio files
        File testDir = new File("scratch");
        File[] files = testDir.listFiles((dir, name) -> name.endsWith(".webm") || name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".ogg"));
        if (files != null) {
            for (File f : files) {
                System.out.println("Found test file: " + f.getName() + " (" + f.length() + " bytes)");
                testUpload(apiKey, f);
            }
        }
    }

    private static void testUpload(String apiKey, File f) {
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("api-subscription-key", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            String filename = f.getName();
            String contentType = "audio/webm";
            if (filename.endsWith(".wav")) contentType = "audio/wav";
            if (filename.endsWith(".mp3")) contentType = "audio/mpeg";
            if (filename.endsWith(".ogg")) contentType = "audio/ogg";

            final String finalContentType = contentType;
            final String finalFilename = filename;

            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(finalContentType));
            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return finalFilename;
                }
            }, fileHeaders);

            body.add("file", fileEntity);
            body.add("model", "saaras:v3");
            body.add("language_code", "hi-IN");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            System.out.println("Sending request to Sarvam for file: " + filename + " with Content-Type: " + contentType);

            ResponseEntity<String> response = restTemplate.postForEntity("https://api.sarvam.ai/speech-to-text", requestEntity, String.class);
            System.out.println("SUCCESS Response: " + response.getBody());
        } catch (HttpStatusCodeException e) {
            System.out.println("ERROR Response (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
