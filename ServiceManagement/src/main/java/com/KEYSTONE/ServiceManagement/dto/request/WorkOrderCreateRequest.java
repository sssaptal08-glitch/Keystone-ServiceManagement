package com.KEYSTONE.ServiceManagement.dto.request;

import com.KEYSTONE.ServiceManagement.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record WorkOrderCreateRequest(
        @NotBlank String title,
        String description,
        Long customerId,
        @NotNull Long siteId,
        @NotNull Priority priority,
        Instant dueAt,
        Long assignedTechnicianId
) {
}
