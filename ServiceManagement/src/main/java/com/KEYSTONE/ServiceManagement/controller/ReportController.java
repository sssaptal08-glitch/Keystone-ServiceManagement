package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.dto.response.ActivityEntryResponse;
import com.KEYSTONE.ServiceManagement.dto.response.DashboardSummaryResponse;
import com.KEYSTONE.ServiceManagement.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public DashboardSummaryResponse dashboard() {
        return reportService.dashboardSummary();
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public List<ActivityEntryResponse> activity(@RequestParam(defaultValue = "50") int limit) {
        return reportService.recentActivity(limit);
    }
}
