package com.KEYSTONE.ServiceManagement.repository;

import com.KEYSTONE.ServiceManagement.domain.Notification;
import com.KEYSTONE.ServiceManagement.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    long countByRecipientIdAndReadFalse(Long recipientId);
    boolean existsByWorkOrderIdAndType(Long workOrderId, NotificationType type);
}
