package com.credlink.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findAllByOrderByCreatedAtDesc();
    Optional<NotificationLog> findByIdAndTransactionIdIsNotNull(Long id);
}
