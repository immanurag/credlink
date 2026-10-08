package com.credlink.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RequestOtpRequest {
    @NotBlank @Email
    private String email;
    private String storeName; // used only on first-time signup

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
}
