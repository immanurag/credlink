package com.credlink.voice;

import com.credlink.common.ApiResponse;
import com.credlink.common.exception.ApiException;
import com.credlink.voice.dto.TranscribeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {

    private final SarvamVoiceService sarvamVoiceService;

    @Value("${credlink.voice.mode:mock}")
    private String voiceMode;

    public VoiceController(SarvamVoiceService sarvamVoiceService) {
        this.sarvamVoiceService = sarvamVoiceService;
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        boolean sarvamReady = sarvamVoiceService.isConfigured();
        return ApiResponse.ok(Map.of(
                "mode", voiceMode,
                "sarvamConfigured", sarvamReady,
                "provider", "sarvam".equalsIgnoreCase(voiceMode) && sarvamReady ? "sarvam" : "web_speech"
        ));
    }

    @PostMapping("/transcribe-json")
    public ApiResponse<Map<String, String>> transcribeJson(@RequestBody TranscribeRequest req) {
        if (req == null || req.getTranscript() == null || req.getTranscript().isBlank()) {
            throw ApiException.badRequest("VOICE_EMPTY_TRANSCRIPT", "No speech was detected.");
        }
        return ApiResponse.ok(Map.of("transcript", req.getTranscript().trim(), "provider", "client"));
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> transcribeAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "languageCode", required = false) String languageCode
    ) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw ApiException.badRequest("VOICE_EMPTY_AUDIO", "Voice recording is empty or invalid. Please record again.");
        }

        if ("sarvam".equalsIgnoreCase(voiceMode) || sarvamVoiceService.isConfigured()) {
            String transcript = sarvamVoiceService.transcribe(file, languageCode);
            return ApiResponse.ok(Map.of("transcript", transcript, "provider", "sarvam"));
        } else {
            throw ApiException.badRequest("VOICE_CONFIG_MISSING", "Sarvam Voice Service is not enabled or SARVAM_API_KEY is missing.");
        }
    }
}
