package com.credlink.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TwilioNotificationServiceTest {

    private TwilioNotificationService service;

    @BeforeEach
    void setup() {
        service = new TwilioNotificationService();
        ReflectionTestUtils.setField(service, "mode", "mock");
        ReflectionTestUtils.setField(service, "accountSid", "");
        ReflectionTestUtils.setField(service, "authToken", "");
        ReflectionTestUtils.setField(service, "smsFrom", "");
        ReflectionTestUtils.setField(service, "whatsappFrom", "");
    }

    @Test
    void mockModeSmsReturnsDeliveredWithoutNetworkCall() {
        var result = service.sendSms("+919810000000", "Test message");
        assertEquals(NotificationStatus.DELIVERED, result.status);
        assertNotNull(result.providerRef);
        assertTrue(result.providerRef.startsWith("mock-"));
    }

    @Test
    void mockModeWhatsAppReturnsDeliveredWithoutNetworkCall() {
        var result = service.sendWhatsApp("+919810000000", "Test message");
        assertEquals(NotificationStatus.DELIVERED, result.status);
        assertNull(result.errorMessage);
    }
}
