package com.credlink.notification.dto;

import com.credlink.notification.NotificationLog;

import java.time.Instant;

public class NotificationLogResponse {
    private Long id;
    private Long transactionId;
    private String channel;
    private String status;
    private String providerRef;
    private String errorMessage;
    private Instant createdAt;

    public static NotificationLogResponse from(NotificationLog l) {
        NotificationLogResponse r = new NotificationLogResponse();
        r.id = l.getId();
        r.transactionId = l.getTransactionId();
        r.channel = l.getChannel().name();
        r.status = l.getStatus().name();
        r.providerRef = l.getProviderRef();
        r.errorMessage = l.getErrorMessage();
        r.createdAt = l.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getTransactionId() { return transactionId; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public String getProviderRef() { return providerRef; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
