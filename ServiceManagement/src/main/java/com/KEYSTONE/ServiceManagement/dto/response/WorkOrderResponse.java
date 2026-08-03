package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.PartUsage;
import com.KEYSTONE.ServiceManagement.domain.Priority;
import com.KEYSTONE.ServiceManagement.domain.WorkOrder;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkOrderResponse(
        Long id,
        String code,
        String title,
        String description,
        Long customerId,
        String customerName,
        Long siteId,
        String siteName,
        Long assignedTechnicianId,
        String assignedTechnicianName,
        WorkOrderStatus status,
        Priority priority,
        Instant dueAt,
        Instant startedAt,
        Instant completedAt,
        Instant closedAt,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt,
        boolean overdue,
        boolean atRisk,
        BigDecimal totalPartsCost,
        int totalMinutesLogged
) {
    private static final java.util.Set<WorkOrderStatus> TERMINAL_OR_DONE = java.util.Set.of(
            WorkOrderStatus.COMPLETED, WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED);

    public static WorkOrderResponse from(WorkOrder wo) {
        boolean stillOpenForSla = !TERMINAL_OR_DONE.contains(wo.getStatus());

        boolean overdue = wo.getDueAt() != null
                && wo.getDueAt().isBefore(Instant.now())
                && stillOpenForSla;

        // "At risk": not yet overdue, but due within the configured at-risk window (F7.2).
        boolean atRisk = !overdue && stillOpenForSla && wo.getDueAt() != null
                && wo.getDueAt().isBefore(Instant.now().plusSeconds(SlaThresholds.AT_RISK_HOURS * 3600));

        BigDecimal totalPartsCost = wo.getPartUsages() == null ? BigDecimal.ZERO
                : wo.getPartUsages().stream()
                        .map(WorkOrderResponse::lineCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalMinutesLogged = wo.getTimeLogs() == null ? 0
                : wo.getTimeLogs().stream()
                        .mapToInt(t -> t.getMinutesLogged() != null ? t.getMinutesLogged() : 0)
                        .sum();

        return new WorkOrderResponse(
                wo.getId(),
                wo.getCode(),
                wo.getTitle(),
                wo.getDescription(),
                wo.getCustomer().getId(),
                wo.getCustomer().getName(),
                wo.getSite().getId(),
                wo.getSite().getName(),
                wo.getAssignedTechnician() != null ? wo.getAssignedTechnician().getId() : null,
                wo.getAssignedTechnician() != null ? wo.getAssignedTechnician().getName() : null,
                wo.getStatus(),
                wo.getPriority(),
                wo.getDueAt(),
                wo.getStartedAt(),
                wo.getCompletedAt(),
                wo.getClosedAt(),
                wo.getResolutionNotes(),
                wo.getCreatedAt(),
                wo.getUpdatedAt(),
                overdue,
                atRisk,
                totalPartsCost,
                totalMinutesLogged
        );
    }

    private static BigDecimal lineCost(PartUsage usage) {
        BigDecimal unitCost = usage.getPart() != null && usage.getPart().getUnitCost() != null
                ? usage.getPart().getUnitCost() : BigDecimal.ZERO;
        return unitCost.multiply(BigDecimal.valueOf(usage.getQuantity()));
    }
}
