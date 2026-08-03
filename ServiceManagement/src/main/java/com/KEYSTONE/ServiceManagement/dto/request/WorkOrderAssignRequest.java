package com.KEYSTONE.ServiceManagement.dto.request;

import jakarta.validation.constraints.NotNull;

public record WorkOrderAssignRequest(
        @NotNull Long technicianId
) {
}
