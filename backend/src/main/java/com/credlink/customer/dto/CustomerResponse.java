package com.credlink.customer.dto;

import com.credlink.customer.Customer;

import java.math.BigDecimal;
import java.time.Instant;

public class CustomerResponse {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String category;
    private Integer trustScore;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance; // positive = Lena Hai (customer owes merchant)
    private Instant createdAt;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.id = c.getId();
        r.name = c.getName();
        r.phone = c.getPhone();
        r.address = c.getAddress();
        r.category = c.getCategory();
        r.trustScore = c.getTrustScore();
        r.creditLimit = c.getCreditLimit();
        r.currentBalance = c.getCurrentBalance();
        r.createdAt = c.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCategory() { return category; }
    public Integer getTrustScore() { return trustScore; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public Instant getCreatedAt() { return createdAt; }
}
