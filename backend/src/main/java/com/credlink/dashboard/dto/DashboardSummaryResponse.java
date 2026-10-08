package com.credlink.dashboard.dto;

import java.math.BigDecimal;

public class DashboardSummaryResponse {
    private BigDecimal totalLenaHai;    // sum of positive customer balances (receivable)
    private BigDecimal totalDenaHai;    // sum of negative customer balances, as a positive number (payable/advance)
    private BigDecimal todaysJama;
    private BigDecimal todaysUdhaar;
    private int todaysTransactionCount;
    private int activeCustomerCount;
    private int newCustomersThisWeek;

    public BigDecimal getTotalLenaHai() { return totalLenaHai; }
    public void setTotalLenaHai(BigDecimal totalLenaHai) { this.totalLenaHai = totalLenaHai; }
    public BigDecimal getTotalDenaHai() { return totalDenaHai; }
    public void setTotalDenaHai(BigDecimal totalDenaHai) { this.totalDenaHai = totalDenaHai; }
    public BigDecimal getTodaysJama() { return todaysJama; }
    public void setTodaysJama(BigDecimal todaysJama) { this.todaysJama = todaysJama; }
    public BigDecimal getTodaysUdhaar() { return todaysUdhaar; }
    public void setTodaysUdhaar(BigDecimal todaysUdhaar) { this.todaysUdhaar = todaysUdhaar; }
    public int getTodaysTransactionCount() { return todaysTransactionCount; }
    public void setTodaysTransactionCount(int todaysTransactionCount) { this.todaysTransactionCount = todaysTransactionCount; }
    public int getActiveCustomerCount() { return activeCustomerCount; }
    public void setActiveCustomerCount(int activeCustomerCount) { this.activeCustomerCount = activeCustomerCount; }
    public int getNewCustomersThisWeek() { return newCustomersThisWeek; }
    public void setNewCustomersThisWeek(int newCustomersThisWeek) { this.newCustomersThisWeek = newCustomersThisWeek; }
}
