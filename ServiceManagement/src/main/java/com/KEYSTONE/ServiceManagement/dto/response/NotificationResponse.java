package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.Notification;
import com.KEYSTONE.ServiceManagement.domain.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        Long workOrderId,
        String workOrderCode,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getWorkOrder() != null ? n.getWorkOrder().getId() : null,
                n.getWorkOrder() != null ? n.getWorkOrder().getCode() : null,
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
