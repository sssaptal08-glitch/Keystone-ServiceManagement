package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.domain.Role;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import com.KEYSTONE.ServiceManagement.dto.response.TechnicianWorkloadResponse;
import com.KEYSTONE.ServiceManagement.dto.response.UserResponse;
import com.KEYSTONE.ServiceManagement.repository.UserRepository;
import com.KEYSTONE.ServiceManagement.repository.WorkOrderRepository;
import com.KEYSTONE.ServiceManagement.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final List<WorkOrderStatus> OPEN_STATUSES =
            List.of(WorkOrderStatus.NEW, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        return UserResponse.from(principal.getUser());
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public List<TechnicianWorkloadResponse> technicians() {
        return userRepository.findByRole(Role.TECHNICIAN).stream()
                .map(tech -> TechnicianWorkloadResponse.from(
                        tech, workOrderRepository.countByAssignedTechnicianIdAndStatusIn(tech.getId(), OPEN_STATUSES)))
                .toList();
    }
}
