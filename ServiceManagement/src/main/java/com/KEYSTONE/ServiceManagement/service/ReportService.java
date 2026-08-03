package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.Priority;
import com.KEYSTONE.ServiceManagement.domain.WorkOrder;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatusHistory;
import com.KEYSTONE.ServiceManagement.dto.response.ActivityEntryResponse;
import com.KEYSTONE.ServiceManagement.dto.response.DashboardSummaryResponse;
import com.KEYSTONE.ServiceManagement.repository.PartRepository;
import com.KEYSTONE.ServiceManagement.repository.WorkOrderRepository;
import com.KEYSTONE.ServiceManagement.repository.WorkOrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final List<WorkOrderStatus> OPEN_STATUSES =
            List.of(WorkOrderStatus.NEW, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    private final WorkOrderRepository workOrderRepository;
    private final PartRepository partRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;

    public DashboardSummaryResponse dashboardSummary() {
        List<WorkOrder> all = workOrderRepository.findAll();

        long open = all.stream().filter(w -> OPEN_STATUSES.contains(w.getStatus())).count();

        long overdue = all.stream()
                .filter(w -> OPEN_STATUSES.contains(w.getStatus()))
                .filter(w -> w.getDueAt() != null && w.getDueAt().isBefore(Instant.now()))
                .count();

        Map<String, Long> byStatus = Arrays.stream(WorkOrderStatus.values())
                .collect(Collectors.toMap(Enum::name, s -> all.stream().filter(w -> w.getStatus() == s).count()));

        Map<String, Long> byPriority = Arrays.stream(Priority.values())
                .collect(Collectors.toMap(Enum::name, p -> all.stream()
                        .filter(w -> OPEN_STATUSES.contains(w.getStatus()) && w.getPriority() == p)
                        .count()));

        // F8.3: "At least one view breaks results down by technician or site." Open work orders
        // grouped by assigned technician (or "Unassigned"), sorted by load descending so the
        // busiest technician shows first — useful at a glance for a dispatcher balancing work.
        Map<String, Long> byTechnician = all.stream()
                .filter(w -> OPEN_STATUSES.contains(w.getStatus()))
                .collect(Collectors.groupingBy(
                        w -> w.getAssignedTechnician() != null ? w.getAssignedTechnician().getName() : "Unassigned",
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        long lowStock = partRepository.findAll().stream()
                .filter(p -> p.getQuantityOnHand() <= p.getReorderThreshold())
                .count();

        // F8.1 / Figure 6: "SLA compliance (30 days)" — the share of resolved (COMPLETED/CLOSED)
        // work orders that finished at or before their due date. Null (not 0% or 100%) when
        // nothing has been resolved yet, since there's nothing to measure compliance against.
        List<WorkOrder> resolved = all.stream()
                .filter(w -> w.getStatus() == WorkOrderStatus.COMPLETED || w.getStatus() == WorkOrderStatus.CLOSED)
                .toList();
        Double slaCompliancePercent = null;
        if (!resolved.isEmpty()) {
            long compliant = resolved.stream()
                    .filter(w -> w.getDueAt() == null || w.getCompletedAt() == null || !w.getCompletedAt().isAfter(w.getDueAt()))
                    .count();
            slaCompliancePercent = Math.round((compliant * 1000.0) / resolved.size()) / 10.0;
        }

        return new DashboardSummaryResponse(open, overdue, byStatus, byPriority, byTechnician, lowStock, slaCompliancePercent);
    }

    /**
     * Recent status-change activity across every work order — the audit-log viewer the brief
     * lists as an optional stretch goal (13.1), built directly from the existing append-only
     * WorkOrderStatusHistory table rather than a separate audit mechanism.
     */
    public List<ActivityEntryResponse> recentActivity(int limit) {
        List<WorkOrderStatusHistory> recent = historyRepository.findAll(
                PageRequest.of(0, Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "changedAt"))).getContent();
        return recent.stream().map(ActivityEntryResponse::from).toList();
    }
}
