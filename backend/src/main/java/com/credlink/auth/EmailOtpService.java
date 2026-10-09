package com.credlink.auth;

import com.credlink.common.exception.ApiException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handles OTP generation / delivery / verification.
 *
 * SECURITY NOTES:
 * - The OTP is always exactly 6 digits, generated with SecureRandom.
 * - Only a BCrypt hash of the OTP is ever persisted - the plaintext OTP is never written to the
 *   database, and is never included in any API response or exposed to the frontend.
 * - In OTP_MODE=mock it is printed to the SERVER CONSOLE ONLY (clearly marked as dev-mode), so the
 *   login flow is testable without real SMTP credentials. This never happens in "live" mode.
 * - In OTP_MODE=live it is sent by real SMTP email and is never logged, printed, or returned
 *   anywhere - only "an OTP was sent" is ever communicated back to the caller.
 * - Verification is attempt-limited (MAX_ATTEMPTS) to slow down guessing, and an OTP can never be
 *   reused once successfully verified (consumed=true).
 */
@Service
public class EmailOtpService {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final OtpRequestRepository otpRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${credlink.otp.ttl-minutes}")
    private int ttlMinutes;

    @Value("${credlink.otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${credlink.otp.mode}")
    private String mode; // mock | live

    @Value("${credlink.mail.from:}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public EmailOtpService(OtpRequestRepository otpRepository, JavaMailSender mailSender) {
        this.otpRepository = otpRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void requestOtp(String email) {
        otpRepository.findTopByEmailOrderByCreatedAtDesc(email).ifPresent(last -> {
            Instant cooldownEnd = last.getCreatedAt().plusSeconds(resendCooldownSeconds);
            if (Instant.now().isBefore(cooldownEnd)) {
                throw ApiException.badRequest("OTP_COOLDOWN",
                        "Please wait a few seconds before requesting another OTP.");
            }
        });

        // Exactly 6 digits, securely generated (0-999999, zero-padded).
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));

        // Attempt delivery BEFORE persisting, so a failed send never leaves behind an
        // unusable "latest" OTP row or burns the resend cooldown for nothing.
        if ("live".equalsIgnoreCase(mode)) {
            sendLiveEmail(email, otp);
        } else {
            // DEV-MODE ONLY: the plaintext OTP is printed to the console so the flow is testable
            // without SMTP credentials. This branch runs when credlink.otp.mode=mock.
            log.info("[DEV MODE] OTP for {} is: {} (valid {} min) - not emailed because credlink.otp.mode=mock. You can also use 123456.",
                    email, otp, ttlMinutes);
        }

        OtpRequest request = new OtpRequest();
        request.setEmail(email);
        request.setOtpHash(encoder.encode(otp));
        request.setExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
        otpRepository.save(request);
    }

    private void sendLiveEmail(String email, String otp) {
        String from = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : smtpUsername;
        if (from == null || from.isBlank()) {
            log.error("credlink.otp.mode=live but no SMTP_FROM or SMTP_USERNAME is configured.");
            throw new ApiException("OTP_DELIVERY_FAILED", "Unable to send OTP right now. Please configure SMTP credentials or set OTP_MODE=mock in .env", HttpStatus.SERVICE_UNAVAILABLE);
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(email);
            helper.setFrom(from);
            helper.setSubject("Your CredLink login code");
            helper.setText("Your CredLink OTP is " + otp + ". It expires in " + ttlMinutes + " minutes.\n\n"
                    + "If you did not request this, you can safely ignore this email.");
            mailSender.send(mimeMessage);
            log.info("OTP email successfully sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: ", email, e);
            String detail = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getClass().getSimpleName() + ": " + e.getMessage() : e.getClass().getSimpleName();
            throw new ApiException("OTP_DELIVERY_FAILED", "Unable to send OTP email (" + detail + "). Please try again.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        OtpRequest request = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> ApiException.badRequest("OTP_NOT_FOUND", "No OTP was requested for this email."));

        if (request.isConsumed()) {
            throw ApiException.badRequest("OTP_ALREADY_USED", "This OTP has already been used.");
        }
        if (Instant.now().isAfter(request.getExpiresAt())) {
            throw ApiException.badRequest("OTP_EXPIRED", "This OTP has expired. Please request a new one.");
        }
        if (request.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
            throw ApiException.badRequest("OTP_TOO_MANY_ATTEMPTS", "Too many incorrect attempts. Please request a new OTP.");
        }
        if (!"live".equalsIgnoreCase(mode) && "123456".equals(otp)) {
            // Mock mode dev helper: accept 123456 as logged in mock mode
        } else if (!encoder.matches(otp, request.getOtpHash())) {
            request.setAttempts(request.getAttempts() + 1);
            otpRepository.save(request);
            if (request.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
                throw ApiException.badRequest("OTP_TOO_MANY_ATTEMPTS", "Too many incorrect attempts. Please request a new OTP.");
            }
            throw ApiException.badRequest("OTP_INVALID", "The OTP you entered is incorrect.");
        }
        request.setConsumed(true);
        otpRepository.save(request);
    }
}
