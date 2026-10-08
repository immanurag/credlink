package com.credlink;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestSarvamFormat {
    public static void main(String[] args) throws Exception {
        // Read SARVAM_API_KEY from .env
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

        System.out.println("Found SARVAM_API_KEY in .env (length: " + apiKey.length() + ")");

        // Generate a 2-second 16kHz 16-bit mono sine wave WAV file in memory
        byte[] wavBytes = createSineWaveWav(16000, 2.0, 440.0);
        System.out.println("Generated test WAV file: " + wavBytes.length + " bytes");

        testSarvamApi(apiKey, wavBytes, "test_audio.wav", "audio/wav");
        testSarvamApi(apiKey, wavBytes, "recording.webm", "audio/webm");
        testSarvamApi(apiKey, wavBytes, "recording.mp3", "audio/mpeg");
    }

    private static void testSarvamApi(String apiKey, byte[] audioBytes, String filename, String mimeType) {
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
            System.out.println("\nTesting Sarvam upload: filename=" + filename + ", Content-Type=" + mimeType);

            ResponseEntity<String> response = restTemplate.postForEntity("https://api.sarvam.ai/speech-to-text", requestEntity, String.class);
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Response: " + response.getBody());
        } catch (HttpStatusCodeException e) {
            System.out.println("HTTP ERROR (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private static byte[] createSineWaveWav(int sampleRate, double durationSec, double freq) throws IOException {
        int numSamples = (int) (sampleRate * durationSec);
        byte[] pcmData = new byte[numSamples * 2]; // 16-bit = 2 bytes per sample

        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * freq / sampleRate;
            short sample = (short) (Math.sin(angle) * 32767 * 0.5); // 50% amplitude
            pcmData[i * 2] = (byte) (sample & 0xff);
            pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        // RIFF header
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + pcmData.length));
        dos.writeBytes("WAVE");

        // fmt chunk
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16)); // Subchunk1Size (16 for PCM)
        dos.writeShort(Short.reverseBytes((short) 1)); // AudioFormat (1 for PCM)
        dos.writeShort(Short.reverseBytes((short) 1)); // NumChannels (1 for mono)
        dos.writeInt(Integer.reverseBytes(sampleRate)); // SampleRate
        dos.writeInt(Integer.reverseBytes(sampleRate * 2)); // ByteRate
        dos.writeShort(Short.reverseBytes((short) 2)); // BlockAlign
        dos.writeShort(Short.reverseBytes((short) 16)); // BitsPerSample

        // data chunk
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(pcmData.length));
        dos.write(pcmData);

        dos.flush();
        return out.toByteArray();
    }
}
