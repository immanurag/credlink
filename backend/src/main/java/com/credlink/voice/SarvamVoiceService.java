package com.credlink.voice;

import com.credlink.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SarvamVoiceService {

    private static final Logger log = LoggerFactory.getLogger(SarvamVoiceService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${credlink.voice.sarvam-api-key:}")
    private String apiKey;

    @Value("${credlink.voice.sarvam-api-url:https://api.sarvam.ai/speech-to-text}")
    private String apiUrl;

    @Value("${credlink.voice.sarvam-model:saaras:v3}")
    private String model;

    public String transcribe(MultipartFile file, String languageCode) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Sarvam STT is enabled but credlink.voice.sarvam-api-key is not configured.");
            throw ApiException.badRequest("VOICE_CONFIG_MISSING", "Sarvam API Key is missing. Please set SARVAM_API_KEY in .env file.");
        }

        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw ApiException.badRequest("VOICE_EMPTY_AUDIO", "Voice recording is empty or invalid. Please record again.");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();

        log.info("VOICE UPLOAD DEBUG - Original Filename: {}, Content Type: {}, Size: {} bytes",
                originalFilename, contentType, size);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("api-subscription-key", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            final String effectiveFilename = (originalFilename != null && !originalFilename.isBlank())
                    ? originalFilename : "recording.webm";

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return effectiveFilename;
                }
            };

            HttpHeaders fileHeaders = new HttpHeaders();
            if (contentType != null && !contentType.isBlank() && !contentType.equalsIgnoreCase("application/octet-stream")) {
                fileHeaders.setContentType(MediaType.parseMediaType(contentType));
            } else {
                if (effectiveFilename.endsWith(".wav")) {
                    fileHeaders.setContentType(MediaType.parseMediaType("audio/wav"));
                } else if (effectiveFilename.endsWith(".mp4") || effectiveFilename.endsWith(".m4a")) {
                    fileHeaders.setContentType(MediaType.parseMediaType("audio/mp4"));
                } else if (effectiveFilename.endsWith(".ogg")) {
                    fileHeaders.setContentType(MediaType.parseMediaType("audio/ogg"));
                } else if (effectiveFilename.endsWith(".mp3")) {
                    fileHeaders.setContentType(MediaType.parseMediaType("audio/mpeg"));
                } else {
                    fileHeaders.setContentType(MediaType.parseMediaType("audio/webm"));
                }
            }

            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(fileResource, fileHeaders);
            body.add("file", fileEntity);
            body.add("model", model);
            if (languageCode != null && !languageCode.isBlank()) {
                body.add("language_code", languageCode);
            } else {
                body.add("language_code", "hi-IN");
            }

            log.info("Sending Sarvam STT Request: file='{}', contentType='{}', model='{}', lang='{}'",
                    effectiveFilename, fileHeaders.getContentType(), model, languageCode);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw ApiException.badRequest("VOICE_TRANSCRIBE_FAILED", "Sarvam API returned error status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("transcript")) {
                String transcript = root.get("transcript").asText().trim();
                log.info("Sarvam STT successfully transcribed audio using model '{}': {}", model, transcript);
                return transcript;
            } else if (root.has("error")) {
                throw ApiException.badRequest("VOICE_TRANSCRIBE_FAILED", root.get("error").asText());
            }

            return response.getBody();

        } catch (HttpStatusCodeException hsce) {
            String errorBody = hsce.getResponseBodyAsString();
            log.error("Sarvam STT HTTP Status:\n{}\n\nSarvam Response:\n{}", hsce.getStatusCode().value(), errorBody);
            throw ApiException.badRequest("VOICE_TRANSCRIBE_FAILED", "Sarvam STT error (" + hsce.getStatusCode().value() + "): " + errorBody);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Failed to call Sarvam Speech-to-Text API: {}", e.getMessage(), e);
            throw ApiException.badRequest("VOICE_TRANSCRIBE_FAILED", "Voice transcription failed: " + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}

