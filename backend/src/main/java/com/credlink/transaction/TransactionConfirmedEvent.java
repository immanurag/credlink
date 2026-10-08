package com.credlink.transaction;

/**
 * Published only AFTER a transaction is committed to the database (see the
 * @TransactionalEventListener(phase = AFTER_COMMIT) consumer in the notification module).
 * This guarantees a notification is never sent for a transaction that later rolled back,
 * and - just as importantly - that a notification failure can never roll back a ledger write.
 */
public class TransactionConfirmedEvent {
    private final Long transactionId;

    public TransactionConfirmedEvent(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getTransactionId() { return transactionId; }
}
