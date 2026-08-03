package com.KEYSTONE.ServiceManagement.dto.response;

import java.util.Map;

public record DashboardSummaryResponse(
        long totalOpenWorkOrders,
        long overdueWorkOrders,
        Map<String, Long> byStatus,
        Map<String, Long> byPriority,
        Map<String, Long> byTechnician,
        long lowStockParts,
        Double slaCompliancePercent
) {
}
