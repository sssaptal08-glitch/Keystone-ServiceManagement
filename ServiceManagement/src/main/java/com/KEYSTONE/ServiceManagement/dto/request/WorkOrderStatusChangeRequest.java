package com.KEYSTONE.ServiceManagement.dto.request;

import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record WorkOrderStatusChangeRequest(
        @NotNull WorkOrderStatus status,
        String note
) {
}
