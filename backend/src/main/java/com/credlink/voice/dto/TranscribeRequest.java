package com.credlink.voice.dto;

public class TranscribeRequest {
    // In the current architecture, speech-to-text happens client-side via the Web Speech API,
    // so this simply accepts and echoes back the transcript. The endpoint exists so the
    // backend can later be extended to accept raw audio and run Whisper server-side without
    // any frontend contract change.
    private String transcript;

    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
}
