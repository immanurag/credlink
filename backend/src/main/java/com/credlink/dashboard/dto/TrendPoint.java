package com.credlink.dashboard.dto;

import java.math.BigDecimal;

public class TrendPoint {
    private String date;
    private BigDecimal udhaar;
    private BigDecimal jama;

    public TrendPoint(String date, BigDecimal udhaar, BigDecimal jama) {
        this.date = date;
        this.udhaar = udhaar;
        this.jama = jama;
    }

    public String getDate() { return date; }
    public BigDecimal getUdhaar() { return udhaar; }
    public BigDecimal getJama() { return jama; }
}
