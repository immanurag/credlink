package com.credlink.nlp.dto;

import jakarta.validation.constraints.NotBlank;

public class NlpExtractionRequest {
    @NotBlank
    private String transcript;

    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
}
