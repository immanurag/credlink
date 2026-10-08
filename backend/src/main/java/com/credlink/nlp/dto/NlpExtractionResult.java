package com.credlink.nlp.dto;

public class NlpExtractionResult {
    private String customerName;
    private Double amount;
    private String type; // UDHAAR | JAMA
    private Double outstandingAmount;
    private double confidence;
    private boolean valid;
    private String rejectionReason;
    private String englishDescription;

    private String intent = "TRANSACTION"; // TRANSACTION | OUTSTANDING_BALANCE_QUERY
    private Boolean customerFound;
    private String responseMessage;

    public static NlpExtractionResult invalid(String reason) {
        NlpExtractionResult r = new NlpExtractionResult();
        r.valid = false;
        r.rejectionReason = reason;
        return r;
    }

    public static NlpExtractionResult queryResult(String customerName, double confidence, String englishDescription) {
        NlpExtractionResult r = new NlpExtractionResult();
        r.intent = "OUTSTANDING_BALANCE_QUERY";
        r.customerName = customerName;
        r.confidence = confidence;
        r.englishDescription = englishDescription;
        r.valid = true;
        return r;
    }

    public static NlpExtractionResult statementQueryResult(String customerName, double confidence, String englishDescription) {
        NlpExtractionResult r = new NlpExtractionResult();
        r.intent = "STATEMENT_QUERY";
        r.customerName = customerName;
        r.confidence = confidence;
        r.englishDescription = englishDescription;
        r.valid = true;
        return r;
    }

    public static NlpExtractionResult of(String customerName, Double amount, String type, double confidence) {
        return of(customerName, amount, type, null, confidence, null);
    }

    public static NlpExtractionResult of(String customerName, Double amount, String type, double confidence, String englishDescription) {
        return of(customerName, amount, type, null, confidence, englishDescription);
    }

    public static NlpExtractionResult of(String customerName, Double amount, String type, Double outstandingAmount, double confidence, String englishDescription) {
        NlpExtractionResult r = new NlpExtractionResult();
        r.intent = "TRANSACTION";
        r.customerName = customerName;
        r.amount = amount;
        r.type = type;
        r.outstandingAmount = outstandingAmount;
        r.confidence = confidence;
        r.englishDescription = englishDescription;
        r.valid = true;
        return r;
    }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public Boolean getCustomerFound() { return customerFound; }
    public void setCustomerFound(Boolean customerFound) { this.customerFound = customerFound; }
    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(Double outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getEnglishDescription() { return englishDescription; }
    public void setEnglishDescription(String englishDescription) { this.englishDescription = englishDescription; }
}

