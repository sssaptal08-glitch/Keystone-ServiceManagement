package com.KEYSTONE.ServiceManagement.dto.request;

import com.KEYSTONE.ServiceManagement.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * F3.3: "Editable while open; immutable once closed/cancelled." Only covers the descriptive
 * fields a dispatcher/manager would correct after the fact (title, description, priority, due
 * date) — assignment and status changes go through their own dedicated endpoints since those
 * have their own authorization and state-machine rules.
 */
public record WorkOrderUpdateRequest(
        @NotBlank String title,
        String description,
        @NotNull Priority priority,
        Instant dueAt
) {
}
