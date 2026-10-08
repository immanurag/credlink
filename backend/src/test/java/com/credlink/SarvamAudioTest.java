package com.credlink;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class SarvamAudioTest {

    @Test
    void testSarvamAudioUpload() throws Exception {
        String apiKey = null;
        File envFile = new File("../.env");
        if (envFile.exists()) {
            List<String> lines = Files.readAllLines(envFile.toPath());
            for (String line : lines) {
                if (line.startsWith("SARVAM_API_KEY=")) {
                    apiKey = line.substring("SARVAM_API_KEY=".length()).trim();
                }
            }
        }

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("SARVAM_API_KEY is not configured in .env file.");
            return;
        }

        byte[] wavBytes = createSineWaveWav(16000, 2.0, 440.0);

        System.out.println("--- TEST A: Raw ByteArrayResource without HttpEntity Content-Type (Spring default application/octet-stream) ---");
        testRawByteArrayResource(apiKey, wavBytes, "recording.webm");

        System.out.println("\n--- TEST B: Wrapped HttpEntity with explicit audio/wav Content-Type ---");
        testWrappedHttpEntity(apiKey, wavBytes, "recording.wav", "audio/wav");

        System.out.println("\n--- TEST C: Wrapped HttpEntity with explicit audio/webm Content-Type ---");
        testWrappedHttpEntity(apiKey, wavBytes, "recording.webm", "audio/webm");
    }

    private void testRawByteArrayResource(String apiKey, byte[] audioBytes, String filename) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("api-subscription-key", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("file", fileResource);
            body.add("model", "saaras:v3");
            body.add("language_code", "hi-IN");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.sarvam.ai/speech-to-text", requestEntity, String.class);
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Response: " + response.getBody());
        } catch (HttpStatusCodeException e) {
            System.out.println("HTTP ERROR (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void testWrappedHttpEntity(String apiKey, byte[] audioBytes, String filename, String mimeType) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("api-subscription-key", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(mimeType));
            HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            }, fileHeaders);

            body.add("file", filePart);
            body.add("model", "saaras:v3");
            body.add("language_code", "hi-IN");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.sarvam.ai/speech-to-text", requestEntity, String.class);
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Response: " + response.getBody());
        } catch (HttpStatusCodeException e) {
            System.out.println("HTTP ERROR (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private byte[] createSineWaveWav(int sampleRate, double durationSec, double freq) throws IOException {
        int numSamples = (int) (sampleRate * durationSec);
        byte[] pcmData = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * freq / sampleRate;
            short sample = (short) (Math.sin(angle) * 32767 * 0.5);
            pcmData[i * 2] = (byte) (sample & 0xff);
            pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + pcmData.length));
        dos.writeBytes("WAVE");
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(sampleRate * 2));
        dos.writeShort(Short.reverseBytes((short) 2));
        dos.writeShort(Short.reverseBytes((short) 16));
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(pcmData.length));
        dos.write(pcmData);
        dos.flush();
        return out.toByteArray();
    }
}
