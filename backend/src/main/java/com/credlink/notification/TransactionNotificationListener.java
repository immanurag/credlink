package com.credlink.notification;

import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.merchant.Merchant;
import com.credlink.merchant.MerchantRepository;
import com.credlink.transaction.Transaction;
import com.credlink.transaction.TransactionConfirmedEvent;
import com.credlink.transaction.TransactionRepository;
import com.credlink.transaction.TransactionType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * Listens for confirmed transactions and fires a WhatsApp/SMS notification.
 * Bound to AFTER_COMMIT so a notification is only ever sent for a transaction that is
 * genuinely and durably saved, and @Async so a slow/failing Twilio call can never block
 * or roll back the HTTP response for the confirm request itself.
 */
@Component
public class TransactionNotificationListener {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final TwilioNotificationService twilioNotificationService;
    private final NotificationLogRepository notificationLogRepository;

    public TransactionNotificationListener(TransactionRepository transactionRepository,
                                            CustomerRepository customerRepository,
                                            MerchantRepository merchantRepository,
                                            TwilioNotificationService twilioNotificationService,
                                            NotificationLogRepository notificationLogRepository) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
        this.twilioNotificationService = twilioNotificationService;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionConfirmed(TransactionConfirmedEvent event) {
        Transaction t = transactionRepository.findById(event.getTransactionId()).orElse(null);
        if (t == null) return;

        Customer customer = customerRepository.findById(t.getCustomerId()).orElse(null);
        Merchant merchant = merchantRepository.findById(t.getMerchantId()).orElse(null);
        if (customer == null || merchant == null || customer.getPhone() == null || customer.getPhone().isBlank()) {
            return; // nothing to notify - a missing/failed notification never affects the ledger, which is already committed.
        }

        String message = buildMessage(merchant.getStoreName(), customer.getName(), t.getType(), t.getAmount(), t.getBalanceAfter());

        var result = twilioNotificationService.sendWhatsApp(customer.getPhone(), message);
        NotificationLog log = new NotificationLog();
        log.setTransactionId(t.getId());
        log.setChannel(NotificationChannel.WHATSAPP);
        log.setStatus(result.status);
        log.setProviderRef(result.providerRef);
        log.setErrorMessage(result.errorMessage);
        notificationLogRepository.save(log);
    }

    private String buildMessage(String storeName, String customerName, TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {
        String action = type == TransactionType.UDHAAR
                ? "udhaar darj kiya gaya"
                : "jama/payment record kiya gaya";
        return String.format(
                "Namaste %s ji, %s par \u20B9%s ka %s. Kul baki rashi: \u20B9%s. Kisi sahayata hetu call karein.",
                customerName, storeName, amount.toPlainString(), action, balanceAfter.toPlainString());
    }
}
