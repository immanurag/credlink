package com.credlink.report.dto;

import com.credlink.transaction.dto.TransactionResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class StatementResponse {
    private Long customerId;
    private String customerName;
    private Instant from;
    private Instant to;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal currentOutstanding;
    private BigDecimal totalUdhaar;
    private BigDecimal totalJama;
    private BigDecimal totalHistoricalUdhaar;
    private BigDecimal totalHistoricalJama;
    private List<TransactionResponse> transactions;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }
    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
    public BigDecimal getCurrentOutstanding() { return currentOutstanding != null ? currentOutstanding : closingBalance; }
    public void setCurrentOutstanding(BigDecimal currentOutstanding) { this.currentOutstanding = currentOutstanding; }
    public BigDecimal getTotalUdhaar() { return totalUdhaar; }
    public void setTotalUdhaar(BigDecimal totalUdhaar) { this.totalUdhaar = totalUdhaar; }
    public BigDecimal getTotalJama() { return totalJama; }
    public void setTotalJama(BigDecimal totalJama) { this.totalJama = totalJama; }
    public BigDecimal getTotalHistoricalUdhaar() { return totalHistoricalUdhaar != null ? totalHistoricalUdhaar : totalUdhaar; }
    public void setTotalHistoricalUdhaar(BigDecimal totalHistoricalUdhaar) { this.totalHistoricalUdhaar = totalHistoricalUdhaar; }
    public BigDecimal getTotalHistoricalJama() { return totalHistoricalJama != null ? totalHistoricalJama : totalJama; }
    public void setTotalHistoricalJama(BigDecimal totalHistoricalJama) { this.totalHistoricalJama = totalHistoricalJama; }
    public List<TransactionResponse> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionResponse> transactions) { this.transactions = transactions; }
}
