package com.credlink.auth;

import com.credlink.auth.dto.AuthTokenResponse;
import com.credlink.common.exception.ApiException;
import com.credlink.merchant.Merchant;
import com.credlink.merchant.MerchantRepository;
import com.credlink.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final EmailOtpService otpService;
    private final JwtService jwtService;

    public AuthService(MerchantRepository merchantRepository, EmailOtpService otpService, JwtService jwtService) {
        this.merchantRepository = merchantRepository;
        this.otpService = otpService;
        this.jwtService = jwtService;
    }

    @Transactional
    public void requestOtp(String rawEmail, String storeName) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw ApiException.badRequest("EMAIL_REQUIRED", "Email address is required.");
        }
        String email = rawEmail.trim().toLowerCase();
        // First-time email: create a lightweight merchant record so verify-otp always has
        // somewhere to attach the session to (store details can be completed later).
        if (!merchantRepository.existsByEmail(email)) {
            Merchant merchant = new Merchant();
            merchant.setEmail(email);
            merchant.setStoreName(storeName != null && !storeName.isBlank() ? storeName.trim() : "My Store");
            merchantRepository.save(merchant);
        }
        otpService.requestOtp(email);
    }

    @Transactional
    public AuthTokenResponse verifyOtp(String rawEmail, String otp) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw ApiException.badRequest("EMAIL_REQUIRED", "Email address is required.");
        }
        String email = rawEmail.trim().toLowerCase();
        otpService.verifyOtp(email, otp);
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "No merchant account found for this email."));

        String access = jwtService.generateAccessToken(merchant.getId(), merchant.getEmail());
        String refresh = jwtService.generateRefreshToken(merchant.getId());
        return new AuthTokenResponse(access, refresh, merchant.getId(), merchant.getStoreName(), merchant.getEmail());
    }

    public AuthTokenResponse refresh(String refreshToken) {
        if (!jwtService.isValid(refreshToken)) {
            throw ApiException.unauthorized("REFRESH_TOKEN_INVALID", "Refresh token is invalid or expired.");
        }
        Long merchantId = jwtService.extractMerchantId(refreshToken);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "Merchant not found."));
        String access = jwtService.generateAccessToken(merchant.getId(), merchant.getEmail());
        String newRefresh = jwtService.generateRefreshToken(merchant.getId());
        return new AuthTokenResponse(access, newRefresh, merchant.getId(), merchant.getStoreName(), merchant.getEmail());
    }
}
