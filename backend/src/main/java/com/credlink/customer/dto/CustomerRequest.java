package com.credlink.customer.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class CustomerRequest {
    @NotBlank
    private String name;
    private String phone;
    private String address;
    private String category;
    private BigDecimal creditLimit;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
}
