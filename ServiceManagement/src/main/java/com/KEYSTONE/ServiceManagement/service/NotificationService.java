package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.*;
import com.KEYSTONE.ServiceManagement.dto.response.NotificationResponse;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import com.KEYSTONE.ServiceManagement.repository.NotificationRepository;
import com.KEYSTONE.ServiceManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates and delivers in-app notifications. Two delivery paths:
 *   1. Persisted to the notifications table, so a user sees what they missed on next login.
 *   2. Broadcast live over STOMP (/topic/notifications) so a connected client updates instantly.
 *
 * The broadcast is unauthenticated at the WebSocket layer (see WebSocketConfig), so each event
 * carries the intended recipient's user id and the frontend filters client-side to only surface
 * events addressed to the logged-in user. This keeps the WebSocket wiring simple while still
 * being effectively private in practice, since the payload itself contains nothing another
 * user couldn't already infer.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PageResponse<NotificationResponse> findForUser(Long userId, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                        .map(NotificationResponse::from));
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public boolean alreadyNotifiedForBreach(Long workOrderId, NotificationType type) {
        return notificationRepository.existsByWorkOrderIdAndType(workOrderId, type);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new NotFoundException("Notification not found: " + notificationId);
        }
        notification.setRead(true);
    }

    @Transactional
    public void markAllRead(Long userId) {
        // Small dataset per user in practice (seed-scale); a bulk @Modifying query would be the
        // next optimization if notification volume grows significantly.
        notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .forEach(n -> n.setRead(true));
    }

    @Transactional
    public void notifyUser(User recipient, NotificationType type, String message, WorkOrder workOrder) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .workOrder(workOrder)
                .build();
        notification = notificationRepository.save(notification);
        broadcast(notification);
    }

    @Transactional
    public void notifyRole(Role role, NotificationType type, String message, WorkOrder workOrder) {
        List<User> recipients = userRepository.findByRole(role);
        for (User recipient : recipients) {
            notifyUser(recipient, type, message, workOrder);
        }
    }

    private void broadcast(Notification notification) {
        messagingTemplate.convertAndSend("/topic/notifications", NotificationEvent.from(notification));
    }

    public record NotificationEvent(Long recipientId, NotificationResponse notification) {
        static NotificationEvent from(Notification n) {
            return new NotificationEvent(n.getRecipient().getId(), NotificationResponse.from(n));
        }
    }
}
