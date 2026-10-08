package com.credlink.auth;

import com.credlink.auth.dto.*;
import com.credlink.common.ApiResponse;
import com.credlink.merchant.Merchant;
import com.credlink.merchant.MerchantRepository;
import com.credlink.security.CurrentMerchant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.transaction.Transaction;
import com.credlink.transaction.TransactionRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MerchantRepository merchantRepository;
    private final CurrentMerchant currentMerchant;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final com.credlink.transaction.TransactionService transactionService;

    public AuthController(AuthService authService, MerchantRepository merchantRepository, CurrentMerchant currentMerchant, CustomerRepository customerRepository, TransactionRepository transactionRepository, com.credlink.transaction.TransactionService transactionService) {
        this.authService = authService;
        this.merchantRepository = merchantRepository;
        this.currentMerchant = currentMerchant;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    @PostMapping("/request-otp")
    public ApiResponse<Map<String, String>> requestOtp(@Valid @RequestBody RequestOtpRequest req) {
        authService.requestOtp(req.getEmail(), req.getStoreName());
        return ApiResponse.ok(Map.of("message", "OTP sent to " + req.getEmail()));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<AuthTokenResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ApiResponse.ok(authService.verifyOtp(req.getEmail(), req.getOtp()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ApiResponse.ok(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout() {
        // Stateless JWT - logout is handled client-side by discarding tokens.
        return ApiResponse.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ApiResponse<Merchant> me() {
        Merchant merchant = merchantRepository.findById(currentMerchant.id())
                .orElseThrow();
        return ApiResponse.ok(merchant);
    }
}
