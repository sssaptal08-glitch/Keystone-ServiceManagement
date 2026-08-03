package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatusHistory;

import java.time.Instant;

public record ActivityEntryResponse(
        Long id,
        Long workOrderId,
        String workOrderCode,
        String workOrderTitle,
        WorkOrderStatus fromStatus,
        WorkOrderStatus toStatus,
        String changedByName,
        String note,
        Instant changedAt
) {
    public static ActivityEntryResponse from(WorkOrderStatusHistory h) {
        return new ActivityEntryResponse(
                h.getId(),
                h.getWorkOrder().getId(),
                h.getWorkOrder().getCode(),
                h.getWorkOrder().getTitle(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getChangedBy().getName(),
                h.getNote(),
                h.getChangedAt()
        );
    }
}
