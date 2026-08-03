package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatusHistory;

import java.time.Instant;

public record WorkOrderStatusHistoryResponse(
        Long id,
        WorkOrderStatus fromStatus,
        WorkOrderStatus toStatus,
        String changedByName,
        String note,
        Instant changedAt
) {
    public static WorkOrderStatusHistoryResponse from(WorkOrderStatusHistory h) {
        return new WorkOrderStatusHistoryResponse(
                h.getId(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getChangedBy().getName(),
                h.getNote(),
                h.getChangedAt()
        );
    }
}
