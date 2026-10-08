package com.credlink.auth.dto;

public class AuthTokenResponse {
    private String accessToken;
    private String refreshToken;
    private Long merchantId;
    private String storeName;
    private String email;

    public AuthTokenResponse(String accessToken, String refreshToken, Long merchantId, String storeName, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.merchantId = merchantId;
        this.storeName = storeName;
        this.email = email;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Long getMerchantId() { return merchantId; }
    public String getStoreName() { return storeName; }
    public String getEmail() { return email; }
}
