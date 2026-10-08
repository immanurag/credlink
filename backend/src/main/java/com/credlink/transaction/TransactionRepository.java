package com.credlink.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdAndMerchantId(Long id, Long merchantId);
    List<Transaction> findByMerchantIdAndCustomerIdAndStatusOrderByCreatedAtAsc(Long merchantId, Long customerId, TransactionStatus status);
    List<Transaction> findByMerchantIdAndStatusOrderByCreatedAtDesc(Long merchantId, TransactionStatus status);
    List<Transaction> findByMerchantIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long merchantId, TransactionStatus status, Instant from, Instant to);
    List<Transaction> findByMerchantIdAndCustomerIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long merchantId, Long customerId, TransactionStatus status, Instant from, Instant to);
}
