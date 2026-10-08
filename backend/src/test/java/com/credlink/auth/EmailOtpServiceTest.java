package com.credlink.auth;

import com.credlink.common.exception.ApiException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailOtpServiceTest {

    private OtpRequestRepository otpRepository;
    private JavaMailSender mailSender;
    private EmailOtpService service;

    @BeforeEach
    void setup() {
        otpRepository = mock(OtpRequestRepository.class);
        mailSender = mock(JavaMailSender.class);
        service = new EmailOtpService(otpRepository, mailSender);
        ReflectionTestUtils.setField(service, "ttlMinutes", 5);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 30);
        ReflectionTestUtils.setField(service, "mode", "mock");
        ReflectionTestUtils.setField(service, "mailFrom", "");
        ReflectionTestUtils.setField(service, "smtpUsername", "");
        when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // A real (unconnected) MimeMessage so MimeMessageHelper has something valid to operate on.
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    // ---- mock mode ----

    @Test
    void mockModeDoesNotCallMailSenderAndPersistsOtp() {
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.empty());
        service.requestOtp("a@b.com");
        verifyNoInteractions(mailSender);
        verify(otpRepository).save(any(OtpRequest.class));
    }

    @Test
    void enforcesResendCooldown() {
        OtpRequest recent = new OtpRequest();
        recent.setEmail("a@b.com");
        ReflectionTestUtils.setField(recent, "createdAt", Instant.now());
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.of(recent));

        ApiException ex = assertThrows(ApiException.class, () -> service.requestOtp("a@b.com"));
        assertEquals("OTP_COOLDOWN", ex.getCode());
    }

    // ---- live mode: SMTP is actually invoked ----

    @Test
    void liveModeSendsRealEmailViaMailSender() {
        ReflectionTestUtils.setField(service, "mode", "live");
        ReflectionTestUtils.setField(service, "smtpUsername", "sender@credlink.app");
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.empty());

        service.requestOtp("a@b.com");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
        verify(otpRepository).save(any(OtpRequest.class));
    }

    @Test
    void liveModePrefersExplicitFromOverSmtpUsername() {
        ReflectionTestUtils.setField(service, "mode", "live");
        ReflectionTestUtils.setField(service, "mailFrom", "otp@credlink.app");
        ReflectionTestUtils.setField(service, "smtpUsername", "raw-smtp-account@gmail.com");
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.empty());

        service.requestOtp("a@b.com");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void liveModeWithNoFromOrUsernameFailsCleanlyWithoutCallingMailSender() {
        ReflectionTestUtils.setField(service, "mode", "live");
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.requestOtp("a@b.com"));
        assertEquals("OTP_DELIVERY_FAILED", ex.getCode());
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(otpRepository, never()).save(any());
    }

    @Test
    void liveModeSmtpFailureReturnsCleanErrorAndDoesNotPersistOtp() {
        ReflectionTestUtils.setField(service, "mode", "live");
        ReflectionTestUtils.setField(service, "smtpUsername", "sender@credlink.app");
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        ApiException ex = assertThrows(ApiException.class, () -> service.requestOtp("a@b.com"));
        assertEquals("OTP_DELIVERY_FAILED", ex.getCode());
        assertEquals("Unable to send OTP email. Please try again.", ex.getMessage());
        // A failed send must never leave behind a usable OTP row or a JWT-issuing side effect.
        verify(otpRepository, never()).save(any());
    }

    // ---- verification ----

    @Test
    void rejectsExpiredOtp() {
        OtpRequest expired = new OtpRequest();
        expired.setEmail("a@b.com");
        expired.setOtpHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.of(expired));

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp("a@b.com", "123456"));
        assertEquals("OTP_EXPIRED", ex.getCode());
    }

    @Test
    void rejectsIncorrectOtp() {
        OtpRequest req = new OtpRequest();
        req.setEmail("a@b.com");
        req.setOtpHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
        req.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.of(req));

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp("a@b.com", "000000"));
        assertEquals("OTP_INVALID", ex.getCode());
        assertEquals(1, req.getAttempts());
    }

    @Test
    void locksOutAfterTooManyIncorrectAttempts() {
        OtpRequest req = new OtpRequest();
        req.setEmail("a@b.com");
        req.setOtpHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
        req.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.of(req));

        for (int i = 0; i < 5; i++) {
            assertThrows(ApiException.class, () -> service.verifyOtp("a@b.com", "000000"));
        }

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp("a@b.com", "123456"));
        assertEquals("OTP_TOO_MANY_ATTEMPTS", ex.getCode());
    }

    @Test
    void acceptsCorrectOtpAndMarksConsumed() {
        OtpRequest req = new OtpRequest();
        req.setEmail("a@b.com");
        req.setOtpHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
        req.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.of(req));

        service.verifyOtp("a@b.com", "123456");
        assertTrue(req.isConsumed());
    }

    @Test
    void otpCannotBeReusedAfterSuccessfulVerification() {
        OtpRequest req = new OtpRequest();
        req.setEmail("a@b.com");
        req.setOtpHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
        req.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(Optional.of(req));

        service.verifyOtp("a@b.com", "123456");
        assertTrue(req.isConsumed());

        // Same "latest OTP" row, now consumed - a second verify attempt (even with the right code)
        // must be rejected instead of silently succeeding again.
        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp("a@b.com", "123456"));
        assertEquals("OTP_ALREADY_USED", ex.getCode());
    }
}
