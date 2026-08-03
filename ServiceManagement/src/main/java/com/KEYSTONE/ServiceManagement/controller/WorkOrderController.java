package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import com.KEYSTONE.ServiceManagement.dto.request.*;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.dto.response.WorkOrderResponse;
import com.KEYSTONE.ServiceManagement.dto.response.WorkOrderStatusHistoryResponse;
import com.KEYSTONE.ServiceManagement.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN','CUSTOMER')")
    public PageResponse<WorkOrderResponse> findAll(@RequestParam(required = false) WorkOrderStatus status,
                                                    @RequestParam(required = false) Long technicianId,
                                                    @RequestParam(required = false) Long customerId,
                                                    @RequestParam(required = false) String search,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    Authentication authentication) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return workOrderService.findAll(status, technicianId, customerId, search, pageable, authentication);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN','CUSTOMER')")
    public WorkOrderResponse findById(@PathVariable Long id, Authentication authentication) {
        return workOrderService.findById(id, authentication);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN','CUSTOMER')")
    public List<WorkOrderStatusHistoryResponse> history(@PathVariable Long id, Authentication authentication) {
        return workOrderService.history(id, authentication);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderCreateRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.create(request, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public WorkOrderResponse update(@PathVariable Long id, @Valid @RequestBody WorkOrderUpdateRequest request,
                                     Authentication authentication) {
        return workOrderService.update(id, request, authentication);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public WorkOrderResponse assign(@PathVariable Long id, @Valid @RequestBody WorkOrderAssignRequest request,
                                     Authentication authentication) {
        return workOrderService.assign(id, request, authentication);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public WorkOrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody WorkOrderStatusChangeRequest request,
                                           Authentication authentication) {
        return workOrderService.changeStatus(id, request, authentication);
    }

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public WorkOrderResponse addPartUsage(@PathVariable Long id, @Valid @RequestBody PartUsageRequest request,
                                           Authentication authentication) {
        return workOrderService.addPartUsage(id, request, authentication);
    }

    @PostMapping("/{id}/clock-in")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public WorkOrderResponse clockIn(@PathVariable Long id, Authentication authentication) {
        return workOrderService.clockIn(id, authentication);
    }

    @PostMapping("/{id}/clock-out")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public WorkOrderResponse clockOut(@PathVariable Long id,
                                       @RequestBody(required = false) ClockOutRequest request,
                                       Authentication authentication) {
        return workOrderService.clockOut(id, request, authentication);
    }
}
