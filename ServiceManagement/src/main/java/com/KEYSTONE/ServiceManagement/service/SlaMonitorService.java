package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.NotificationType;
import com.KEYSTONE.ServiceManagement.domain.Role;
import com.KEYSTONE.ServiceManagement.domain.WorkOrder;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import com.KEYSTONE.ServiceManagement.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Periodically scans open work orders against their SLA due date (F7.2: "A scheduled job flags
 * work orders at risk of, or in, breach"). Two tiers:
 *   - AT RISK: due within the configured at-risk window but not yet overdue — an early warning.
 *   - BREACHED: already past due while still open.
 * Both create a persisted, live-broadcast notification for every MANAGER (F7.3) rather than only
 * a server log line, and each work order is only notified once per tier so a stuck job doesn't
 * spam managers every scheduling interval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaMonitorService {

    private static final List<WorkOrderStatus> OPEN_STATUSES =
            List.of(WorkOrderStatus.NEW, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    private final WorkOrderRepository workOrderRepository;
    private final NotificationService notificationService;
    private final SlaProperties slaProperties;

    @Scheduled(fixedRateString = "${app.sla.check-interval-ms:300000}")
    @Transactional
    public void checkForBreaches() {
        Instant now = Instant.now();
        Instant riskHorizon = now.plusSeconds(slaProperties.getAtRiskHours() * 3600);

        // Everything due before the risk horizon: this includes both already-breached work
        // orders and ones approaching their deadline — split them apart below.
        List<WorkOrder> dueSoonOrOverdue = workOrderRepository.findByStatusInAndDueAtBefore(OPEN_STATUSES, riskHorizon);

        List<WorkOrder> breached = dueSoonOrOverdue.stream().filter(w -> w.getDueAt().isBefore(now)).toList();
        List<WorkOrder> atRisk = dueSoonOrOverdue.stream().filter(w -> !w.getDueAt().isBefore(now)).toList();

        if (!breached.isEmpty()) {
            log.warn("SLA breach detected on {} work order(s): {}", breached.size(),
                    breached.stream().map(WorkOrder::getCode).toList());
        }

        for (WorkOrder workOrder : breached) {
            if (notificationService.alreadyNotifiedForBreach(workOrder.getId(), NotificationType.SLA_BREACH)) {
                continue;
            }
            String message = "SLA breach: " + workOrder.getCode() + " (" + workOrder.getPriority() + ") — \""
                    + workOrder.getTitle() + "\" is past its due date.";
            notificationService.notifyRole(Role.MANAGER, NotificationType.SLA_BREACH, message, workOrder);
        }

        for (WorkOrder workOrder : atRisk) {
            if (notificationService.alreadyNotifiedForBreach(workOrder.getId(), NotificationType.SLA_AT_RISK)) {
                continue;
            }
            String message = "SLA at risk: " + workOrder.getCode() + " (" + workOrder.getPriority() + ") — \""
                    + workOrder.getTitle() + "\" is due within " + slaProperties.getAtRiskHours() + "h.";
            notificationService.notifyRole(Role.MANAGER, NotificationType.SLA_AT_RISK, message, workOrder);
        }
    }
}
