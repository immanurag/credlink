package com.credlink.common;

import java.time.Instant;

public class ApiError {
    private String code;
    private String message;
    private Instant timestamp = Instant.now();
    private String path;

    public ApiError() {}

    public ApiError(String code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getTimestamp() { return timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
