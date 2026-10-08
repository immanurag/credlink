package com.credlink.notification;

import com.credlink.common.ApiResponse;
import com.credlink.common.exception.ApiException;
import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.notification.dto.NotificationLogResponse;
import com.credlink.security.CurrentMerchant;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;
    private final CustomerRepository customerRepository;
    private final TwilioNotificationService twilioNotificationService;
    private final CurrentMerchant currentMerchant;

    public NotificationController(NotificationLogRepository notificationLogRepository,
                                   CustomerRepository customerRepository,
                                   TwilioNotificationService twilioNotificationService,
                                   CurrentMerchant currentMerchant) {
        this.notificationLogRepository = notificationLogRepository;
        this.customerRepository = customerRepository;
        this.twilioNotificationService = twilioNotificationService;
        this.currentMerchant = currentMerchant;
    }

    @GetMapping
    public ApiResponse<List<NotificationLogResponse>> list() {
        return ApiResponse.ok(notificationLogRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(NotificationLogResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<NotificationLogResponse> get(@PathVariable Long id) {
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "Notification log was not found."));
        return ApiResponse.ok(NotificationLogResponse.from(log));
    }

    /** Manual "Send WhatsApp Reminder" action from the customer ledger / dashboard overdue list. */
    @PostMapping("/customers/{customerId}/remind")
    public ApiResponse<Map<String, String>> remindOne(@PathVariable Long customerId) {
        Customer c = customerRepository.findByIdAndMerchantId(customerId, currentMerchant.id())
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer was not found."));
        sendReminder(c);
        return ApiResponse.ok(Map.of("message", "Reminder sent to " + c.getName()));
    }

    /** "Send Bulk WhatsApp Payment Reminders" from the dashboard's Top Outstanding Due panel. */
    @PostMapping("/customers/remind-overdue")
    public ApiResponse<Map<String, Object>> remindAllOverdue() {
        List<Customer> overdue = customerRepository.findByMerchantId(currentMerchant.id()).stream()
                .filter(c -> c.getCurrentBalance() != null && c.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0)
                .filter(c -> c.getPhone() != null && !c.getPhone().isBlank())
                .toList();
        overdue.forEach(this::sendReminder);
        return ApiResponse.ok(Map.of("remindersSent", overdue.size()));
    }

    private void sendReminder(Customer c) {
        String message = String.format(
                "Namaste %s ji, aapka baki rashi \u20B9%s hai. Kripya jaldi bhugtan karein.",
                c.getName(), c.getCurrentBalance().toPlainString());
        var result = twilioNotificationService.sendWhatsApp(c.getPhone(), message);
        NotificationLog logEntry = new NotificationLog();
        logEntry.setTransactionId(0L); // reminders aren't tied to a single transaction
        logEntry.setChannel(NotificationChannel.WHATSAPP);
        logEntry.setStatus(result.status);
        logEntry.setProviderRef(result.providerRef);
        logEntry.setErrorMessage(result.errorMessage);
        notificationLogRepository.save(logEntry);
    }
}
