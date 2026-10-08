package com.credlink.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Thin wrapper around the Twilio SDK. In "mock" mode (default, and whenever no credentials
 * are configured) it logs what WOULD be sent instead of making a network call - so the whole
 * ledger + review + confirm flow is fully testable without a Twilio account.
 * WhatsApp messages MUST use the "whatsapp:" prefix on both from and to numbers, or Twilio
 * silently misroutes them as plain SMS.
 */
@Service
public class TwilioNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TwilioNotificationService.class);

    @Value("${credlink.notification.mode}")
    private String mode; // mock | live

    @Value("${credlink.notification.twilio.account-sid}")
    private String accountSid;

    @Value("${credlink.notification.twilio.auth-token}")
    private String authToken;

    @Value("${credlink.notification.twilio.sms-from}")
    private String smsFrom;

    @Value("${credlink.notification.twilio.whatsapp-from}")
    private String whatsappFrom;

    @PostConstruct
    public void init() {
        if ("live".equalsIgnoreCase(mode) && accountSid != null && !accountSid.isBlank()) {
            Twilio.init(accountSid, authToken);
        }
    }

    public DeliveryResult sendSms(String toPhone, String body) {
        if (!"live".equalsIgnoreCase(mode)) {
            log.info("[MOCK SMS] to={} body=\"{}\"", toPhone, body);
            return DeliveryResult.mocked();
        }
        try {
            Message message = Message.creator(new PhoneNumber(toPhone), new PhoneNumber(smsFrom), body).create();
            return DeliveryResult.delivered(message.getSid());
        } catch (Exception e) {
            log.error("Twilio SMS send failed: {}", e.getMessage());
            return DeliveryResult.failed(e.getMessage());
        }
    }

    public DeliveryResult sendWhatsApp(String toPhone, String body) {
        if (!"live".equalsIgnoreCase(mode)) {
            log.info("[MOCK WHATSAPP] to={} body=\"{}\"", toPhone, body);
            return DeliveryResult.mocked();
        }
        try {
            // Correct WhatsApp addressing: BOTH from and to need the "whatsapp:" scheme prefix.
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + toPhone),
                    new PhoneNumber("whatsapp:" + whatsappFrom),
                    body
            ).create();
            return DeliveryResult.delivered(message.getSid());
        } catch (Exception e) {
            log.error("Twilio WhatsApp send failed: {}", e.getMessage());
            return DeliveryResult.failed(e.getMessage());
        }
    }

    public static class DeliveryResult {
        public final NotificationStatus status;
        public final String providerRef;
        public final String errorMessage;

        private DeliveryResult(NotificationStatus status, String providerRef, String errorMessage) {
            this.status = status;
            this.providerRef = providerRef;
            this.errorMessage = errorMessage;
        }

        static DeliveryResult delivered(String sid) { return new DeliveryResult(NotificationStatus.DELIVERED, sid, null); }
        static DeliveryResult mocked() { return new DeliveryResult(NotificationStatus.DELIVERED, "mock-" + System.currentTimeMillis(), null); }
        static DeliveryResult failed(String error) { return new DeliveryResult(NotificationStatus.FAILED, null, error); }
    }
}
