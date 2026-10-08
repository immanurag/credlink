package com.credlink.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Used both for manual entry and for the reviewed/edited output of the voice+NLP pipeline. */
public class TransactionPreviewRequest {
    @NotNull
    private Long customerId;
    @NotBlank
    private String type; // UDHAAR | JAMA
    @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;
    private String description;
    private String source; // VOICE | MANUAL | UPI
    private String voiceTranscript;
    private Double confidenceScore;
    private BigDecimal explicitOutstanding;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getVoiceTranscript() { return voiceTranscript; }
    public void setVoiceTranscript(String voiceTranscript) { this.voiceTranscript = voiceTranscript; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public BigDecimal getExplicitOutstanding() { return explicitOutstanding; }
    public void setExplicitOutstanding(BigDecimal explicitOutstanding) { this.explicitOutstanding = explicitOutstanding; }
}
