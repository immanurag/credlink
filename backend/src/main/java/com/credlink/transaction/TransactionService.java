package com.credlink.transaction;

import com.credlink.common.exception.ApiException;
import com.credlink.customer.Customer;
import com.credlink.customer.CustomerRepository;
import com.credlink.transaction.dto.TransactionPreviewRequest;
import com.credlink.transaction.dto.TransactionResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * The ledger engine. UDHAAR increases the customer's outstanding balance (they owe more),
 * JAMA decreases it (they've paid some back). Balance math and the transaction row are
 * always written atomically in the same DB transaction.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository,
                               CustomerRepository customerRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Creates a PENDING_REVIEW transaction row - used for both the voice review screen and manual entry review. */
    @Transactional
    public TransactionResponse preview(Long merchantId, TransactionPreviewRequest req) {
        Customer customer = customerRepository.findByIdAndMerchantId(req.getCustomerId(), merchantId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer was not found."));

        TransactionType type = parseType(req.getType());
        validateAmount(req.getAmount());

        String description = normalizeDescription(req.getDescription(), req.getVoiceTranscript(), type, req.getAmount(), customer.getName());

        BigDecimal explicitOutstanding = req.getExplicitOutstanding();
        if (explicitOutstanding == null) {
            explicitOutstanding = extractExplicitOutstandingFromText(description, req.getVoiceTranscript());
        }

        Transaction t = new Transaction();
        t.setMerchantId(merchantId);
        t.setCustomerId(customer.getId());
        t.setType(type);
        t.setAmount(req.getAmount());
        t.setDescription(description);
        t.setSource(parseSource(req.getSource()));
        t.setVoiceTranscript(req.getVoiceTranscript());
        t.setConfidenceScore(req.getConfidenceScore());
        t.setExplicitOutstanding(explicitOutstanding);
        t.setStatus(TransactionStatus.PENDING_REVIEW);
        // Show the merchant what the balance WOULD become, without committing it yet.
        t.setBalanceAfter(calculateNewBalance(customer.getCurrentBalance(), type, req.getAmount(), explicitOutstanding));

        return TransactionResponse.from(transactionRepository.save(t));
    }

    /** Commits a previously-previewed transaction: updates the running balance and fires the notification event. */
    @Transactional
    public TransactionResponse confirm(Long merchantId, Long transactionId, TransactionPreviewRequest edits) {
        Transaction t = transactionRepository.findByIdAndMerchantId(transactionId, merchantId)
                .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND", "Transaction was not found."));

        if (t.getStatus() == TransactionStatus.CONFIRMED) {
            throw ApiException.conflict("TRANSACTION_ALREADY_CONFIRMED", "This transaction has already been confirmed.");
        }
        if (t.getStatus() == TransactionStatus.DISCARDED) {
            throw ApiException.conflict("TRANSACTION_DISCARDED", "This transaction was discarded and cannot be confirmed.");
        }

        // Allow last-second edits from the review screen (merchant may correct amount/type/customer before confirming).
        if (edits != null) {
            if (edits.getType() != null) t.setType(parseType(edits.getType()));
            if (edits.getAmount() != null) { validateAmount(edits.getAmount()); t.setAmount(edits.getAmount()); }
            if (edits.getDescription() != null) t.setDescription(edits.getDescription());
            if (edits.getExplicitOutstanding() != null) t.setExplicitOutstanding(edits.getExplicitOutstanding());
        }

        if (t.getExplicitOutstanding() == null) {
            t.setExplicitOutstanding(extractExplicitOutstandingFromText(t.getDescription(), t.getVoiceTranscript()));
        }

        Customer customer = customerRepository.findByIdAndMerchantId(t.getCustomerId(), merchantId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer was not found."));

        BigDecimal newBalance = calculateNewBalance(customer.getCurrentBalance(), t.getType(), t.getAmount(), t.getExplicitOutstanding());
        customer.setCurrentBalance(newBalance);
        customerRepository.save(customer);

        t.setBalanceAfter(newBalance);
        t.setStatus(TransactionStatus.CONFIRMED);
        t.setConfirmedAt(Instant.now());
        Transaction saved = transactionRepository.save(t);

        // Published now, but only actually delivered after this DB transaction commits (see notification module).
        eventPublisher.publishEvent(new TransactionConfirmedEvent(saved.getId()));

        return TransactionResponse.from(saved);
    }

    @Transactional
    public void discard(Long merchantId, Long transactionId) {
        Transaction t = transactionRepository.findByIdAndMerchantId(transactionId, merchantId)
                .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND", "Transaction was not found."));
        if (t.getStatus() == TransactionStatus.CONFIRMED) {
            throw ApiException.conflict("TRANSACTION_ALREADY_CONFIRMED", "Cannot discard a confirmed transaction.");
        }
        t.setStatus(TransactionStatus.DISCARDED);
        transactionRepository.save(t);
    }

    public TransactionResponse get(Long merchantId, Long transactionId) {
        return TransactionResponse.from(
                transactionRepository.findByIdAndMerchantId(transactionId, merchantId)
                        .orElseThrow(() -> ApiException.notFound("TRANSACTION_NOT_FOUND", "Transaction was not found.")));
    }

    public List<TransactionResponse> historyForCustomer(Long merchantId, Long customerId) {
        return transactionRepository
                .findByMerchantIdAndCustomerIdAndStatusOrderByCreatedAtAsc(merchantId, customerId, TransactionStatus.CONFIRMED)
                .stream().map(TransactionResponse::from).toList();
    }

    public List<TransactionResponse> recentForMerchant(Long merchantId, int limit) {
        return transactionRepository.findByMerchantIdAndStatusOrderByCreatedAtDesc(merchantId, TransactionStatus.CONFIRMED)
                .stream().limit(limit).map(TransactionResponse::from).toList();
    }

    public List<Transaction> confirmedInRange(Long merchantId, Instant from, Instant to) {
        return transactionRepository.findByMerchantIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                merchantId, TransactionStatus.CONFIRMED, from, to);
    }

    public List<Transaction> confirmedForCustomerInRange(Long merchantId, Long customerId, Instant from, Instant to) {
        return transactionRepository.findByMerchantIdAndCustomerIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                merchantId, customerId, TransactionStatus.CONFIRMED, from, to);
    }

    private BigDecimal calculateNewBalance(BigDecimal currentBalance, TransactionType type, BigDecimal amount, BigDecimal explicitOutstanding) {
        if (explicitOutstanding != null && explicitOutstanding.compareTo(BigDecimal.ZERO) >= 0) {
            return explicitOutstanding;
        }
        return applyDelta(currentBalance, type, amount);
    }

    private BigDecimal applyDelta(BigDecimal currentBalance, TransactionType type, BigDecimal amount) {
        return type == TransactionType.UDHAAR
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);
    }

    public BigDecimal extractExplicitOutstandingFromText(String desc, String transcript) {
        if (desc != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:Outstanding balance:|baki:|baaki:)\\s*(\\d+(?:\\.\\d+)?)").matcher(desc);
            if (m.find()) {
                try { return new BigDecimal(m.group(1)); } catch (Exception ignored) {}
            }
        }
        if (transcript != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:baki|baaki|bakaya|due|outstanding|बाकी|बकाया)").matcher(transcript);
            if (m.find()) {
                try { return new BigDecimal(m.group(1)); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @Transactional
    public BigDecimal recalculateCustomerBalance(Long merchantId, Long customerId) {
        List<Transaction> txs = transactionRepository
                .findByMerchantIdAndCustomerIdAndStatusOrderByCreatedAtAsc(merchantId, customerId, TransactionStatus.CONFIRMED);

        if (txs.isEmpty()) {
            Customer customer = customerRepository.findByIdAndMerchantId(customerId, merchantId).orElse(null);
            return customer != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        }

        for (Transaction t : txs) {
            if (t.getExplicitOutstanding() == null) {
                BigDecimal parsed = extractExplicitOutstandingFromText(t.getDescription(), t.getVoiceTranscript());
                if (parsed != null) {
                    t.setExplicitOutstanding(parsed);
                }
            }
        }

        int anchorIdx = -1;
        for (int i = 0; i < txs.size(); i++) {
            if (txs.get(i).getExplicitOutstanding() != null && txs.get(i).getExplicitOutstanding().compareTo(BigDecimal.ZERO) >= 0) {
                anchorIdx = i;
                break;
            }
        }

        BigDecimal startingBalance = BigDecimal.ZERO;
        if (anchorIdx >= 0) {
            BigDecimal nextBal = txs.get(anchorIdx).getExplicitOutstanding();
            for (int i = anchorIdx; i >= 0; i--) {
                Transaction t = txs.get(i);
                if (i < anchorIdx && t.getExplicitOutstanding() != null) {
                    nextBal = t.getExplicitOutstanding();
                } else {
                    nextBal = (t.getType() == TransactionType.JAMA)
                            ? nextBal.add(t.getAmount())
                            : nextBal.subtract(t.getAmount());
                }
            }
            startingBalance = nextBal.compareTo(BigDecimal.ZERO) >= 0 ? nextBal : BigDecimal.ZERO;
        }

        BigDecimal runningBalance = startingBalance;
        for (Transaction t : txs) {
            runningBalance = calculateNewBalance(runningBalance, t.getType(), t.getAmount(), t.getExplicitOutstanding());
            t.setBalanceAfter(runningBalance);
            transactionRepository.save(t);
        }

        Customer customer = customerRepository.findByIdAndMerchantId(customerId, merchantId).orElse(null);
        if (customer != null) {
            customer.setCurrentBalance(runningBalance);
            customerRepository.save(customer);
        }
        return runningBalance;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Transaction amount must be greater than zero.");
        }
    }

    private TransactionType parseType(String raw) {
        try {
            return TransactionType.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("INVALID_TRANSACTION_TYPE", "Type must be UDHAAR or JAMA.");
        }
    }

    private TransactionSource parseSource(String raw) {
        if (raw == null || raw.isBlank()) return TransactionSource.MANUAL;
        try {
            return TransactionSource.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return TransactionSource.MANUAL;
        }
    }

    private String normalizeDescription(String description, String voiceTranscript, TransactionType type, BigDecimal amount, String customerName) {
        if (description != null && !description.isBlank() && !description.equals(voiceTranscript) && !containsNonAscii(description)) {
            return description;
        }
        String formattedAmount = amount != null ? amount.stripTrailingZeros().toPlainString() : "0";
        String name = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        if (type == TransactionType.UDHAAR) {
            return "Credit of " + formattedAmount + " rupees given to " + name;
        } else {
            return "Payment of " + formattedAmount + " rupees received from " + name;
        }
    }

    private boolean containsNonAscii(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            if (c > 127) return true;
        }
        return false;
    }
}

