package com.credlink.transaction.dto;

import com.credlink.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionResponse {
    private Long id;
    private Long customerId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private String source;
    private String voiceTranscript;
    private Double confidenceScore;
    private BigDecimal explicitOutstanding;
    private String status;
    private Instant createdAt;
    private Instant confirmedAt;

    public static TransactionResponse from(Transaction t) {
        TransactionResponse r = new TransactionResponse();
        r.id = t.getId();
        r.customerId = t.getCustomerId();
        r.type = t.getType() != null ? t.getType().name() : null;
        r.amount = t.getAmount();
        r.balanceAfter = t.getBalanceAfter();
        r.description = t.getDescription();
        r.source = t.getSource() != null ? t.getSource().name() : null;
        r.voiceTranscript = t.getVoiceTranscript();
        r.confidenceScore = t.getConfidenceScore();
        r.explicitOutstanding = t.getExplicitOutstanding();
        r.status = t.getStatus() != null ? t.getStatus().name() : null;
        r.createdAt = t.getCreatedAt();
        r.confirmedAt = t.getConfirmedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
    public String getVoiceTranscript() { return voiceTranscript; }
    public Double getConfidenceScore() { return confidenceScore; }
    public BigDecimal getExplicitOutstanding() { return explicitOutstanding; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
