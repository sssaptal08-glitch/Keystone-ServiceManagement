package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.*;
import com.KEYSTONE.ServiceManagement.dto.request.PartUsageRequest;
import com.KEYSTONE.ServiceManagement.dto.request.WorkOrderCreateRequest;
import com.KEYSTONE.ServiceManagement.dto.request.WorkOrderStatusChangeRequest;
import com.KEYSTONE.ServiceManagement.dto.request.WorkOrderUpdateRequest;
import com.KEYSTONE.ServiceManagement.exception.InsufficientStockException;
import com.KEYSTONE.ServiceManagement.exception.InvalidStateTransitionException;
import com.KEYSTONE.ServiceManagement.repository.*;
import com.KEYSTONE.ServiceManagement.security.SecurityUser;
import com.KEYSTONE.ServiceManagement.util.WorkOrderCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the work order status state machine, inventory rules, and — critically —
 * object-level authorization (a technician acting only on their own jobs, a customer viewing
 * only their own organisation's data, and CLOSE being manager-only). No Spring context is
 * loaded — repositories are mocked directly.
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderStatusHistoryRepository historyRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private UserRepository userRepository;
    @Mock private PartRepository partRepository;
    @Mock private PartUsageRepository partUsageRepository;
    @Mock private TimeLogRepository timeLogRepository;
    @Mock private WorkOrderCodeGenerator codeGenerator;
    @Mock private WorkOrderEventBroadcaster eventBroadcaster;
    @Mock private NotificationService notificationService;

    @Mock private Authentication authentication;

    private WorkOrderService workOrderService;

    private User dispatcher;
    private User manager;
    private User assignedTechnician;
    private User otherTechnician;
    private Customer customer;
    private Site site;
    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderService(
                workOrderRepository, historyRepository, customerRepository, siteRepository,
                userRepository, partRepository, partUsageRepository, timeLogRepository, codeGenerator,
                eventBroadcaster, notificationService, new SlaProperties(4, 8, 24, 72, 2));

        dispatcher = User.builder()
                .id(2L).name("Derek Dispatcher").email("dispatcher@keystone.example")
                .role(Role.DISPATCHER).enabled(true).build();

        manager = User.builder()
                .id(1L).name("Maria Manager").email("manager@keystone.example")
                .role(Role.MANAGER).enabled(true).build();

        assignedTechnician = User.builder()
                .id(3L).name("Tom Technician").email("tom.tech@keystone.example")
                .role(Role.TECHNICIAN).enabled(true).build();

        otherTechnician = User.builder()
                .id(4L).name("Nina Technician").email("nina.tech@keystone.example")
                .role(Role.TECHNICIAN).enabled(true).build();

        lenient().when(authentication.getPrincipal()).thenReturn(new SecurityUser(dispatcher));

        customer = Customer.builder().id(1L).name("Acme Manufacturing").build();
        site = Site.builder().id(1L).customer(customer).name("Plant 1").address("100 Main St").build();

        workOrder = WorkOrder.builder()
                .id(10L).code("WO-2026-TEST1").title("Test job")
                .customer(customer).site(site).createdBy(dispatcher)
                .assignedTechnician(assignedTechnician)
                .status(WorkOrderStatus.NEW).priority(Priority.MEDIUM)
                .build();

        lenient().when(workOrderRepository.findById(10L)).thenReturn(Optional.of(workOrder));
        lenient().when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Authentication authAs(User user) {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(new SecurityUser(user));
        return auth;
    }

    // --- state machine ---

    @Test
    void allowsLegalTransitionFromNewToAssigned() {
        workOrder.setStatus(WorkOrderStatus.NEW);

        var response = workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.ASSIGNED, "assigning"), authentication);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
    }

    @Test
    void rejectsIllegalTransitionFromNewToInProgress() {
        workOrder.setStatus(WorkOrderStatus.NEW);

        assertThatThrownBy(() -> workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.IN_PROGRESS, null), authentication))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void rejectsAnyTransitionOutOfClosed() {
        workOrder.setStatus(WorkOrderStatus.CLOSED);

        // Even a manager cannot move a CLOSED work order — it's terminal.
        assertThatThrownBy(() -> workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.IN_PROGRESS, null), authAs(manager)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void completingSetsCompletedTimestamp() {
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrder.setAssignedTechnician(assignedTechnician);

        var response = workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.COMPLETED, "done"), authentication);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
    }

    // --- object-level authorization ---

    @Test
    void onlyManagerCanCloseAWorkOrder() {
        workOrder.setStatus(WorkOrderStatus.COMPLETED);

        // Dispatcher attempting to close must be rejected...
        assertThatThrownBy(() -> workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.CLOSED, null), authentication))
                .isInstanceOf(AccessDeniedException.class);

        // ...but a manager can.
        var response = workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.CLOSED, null), authAs(manager));
        assertThat(response.status()).isEqualTo(WorkOrderStatus.CLOSED);
    }

    @Test
    void technicianCannotActOnAWorkOrderAssignedToSomeoneElse() {
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        workOrder.setAssignedTechnician(assignedTechnician);

        assertThatThrownBy(() -> workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.IN_PROGRESS, null), authAs(otherTechnician)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignedTechnicianCanActOnTheirOwnWorkOrder() {
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        workOrder.setAssignedTechnician(assignedTechnician);

        var response = workOrderService.changeStatus(10L,
                new WorkOrderStatusChangeRequest(WorkOrderStatus.IN_PROGRESS, "starting"), authAs(assignedTechnician));

        assertThat(response.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
    }

    @Test
    void customerCannotViewAnotherCustomersWorkOrder() {
        Customer otherCustomer = Customer.builder().id(2L).name("Northwind Retail Group").build();
        User otherCustomerUser = User.builder()
                .id(5L).name("Carla Customer").role(Role.CUSTOMER).customer(otherCustomer).enabled(true).build();

        assertThatThrownBy(() -> workOrderService.findById(10L, authAs(otherCustomerUser)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void customerCanViewTheirOwnOrganisationsWorkOrder() {
        User ownCustomerUser = User.builder()
                .id(5L).name("Carla Customer").role(Role.CUSTOMER).customer(customer).enabled(true).build();

        var response = workOrderService.findById(10L, authAs(ownCustomerUser));
        assertThat(response.id()).isEqualTo(10L);
    }

    // --- parts / inventory ---

    @Test
    void addingPartUsageDecrementsStockWhenSufficient() {
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        Part part = Part.builder().id(5L).sku("BRG-1001").name("Bearing")
                .unitCost(BigDecimal.TEN).quantityOnHand(10).reorderThreshold(2).build();
        when(partRepository.findById(5L)).thenReturn(Optional.of(part));

        workOrderService.addPartUsage(10L, new PartUsageRequest(5L, 4), authentication);

        assertThat(part.getQuantityOnHand()).isEqualTo(6);
    }

    @Test
    void addingPartUsageRejectsWhenStockInsufficient() {
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        Part part = Part.builder().id(5L).sku("BRG-1001").name("Bearing")
                .unitCost(BigDecimal.TEN).quantityOnHand(2).reorderThreshold(2).build();
        when(partRepository.findById(5L)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> workOrderService.addPartUsage(10L, new PartUsageRequest(5L, 5), authentication))
                .isInstanceOf(InsufficientStockException.class);

        // Stock must be left untouched when the request is rejected.
        assertThat(part.getQuantityOnHand()).isEqualTo(2);
    }

    // --- F9: customer self-service requests ---

    @Test
    void customerCreatingWorkOrderIsForcedToOwnOrganisationRegardlessOfRequestedCustomerId() {
        User customerUser = User.builder()
                .id(5L).name("Carla Customer").role(Role.CUSTOMER).customer(customer).enabled(true).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(codeGenerator.generate()).thenReturn("WO-2026-NEWJOB");
        when(workOrderRepository.findByCode(any())).thenReturn(Optional.empty());
        when(workOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Even though this request claims a different (nonexistent) customerId=999, the service
        // must ignore it and use the caller's own organisation.
        var request = new WorkOrderCreateRequest("Leaking valve", "desc", 999L, 1L, Priority.MEDIUM, null, null);
        var response = workOrderService.create(request, authAs(customerUser));

        assertThat(response.customerId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(WorkOrderStatus.NEW);
    }

    @Test
    void autoComputesDueDateFromPriorityWhenNotSupplied() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(codeGenerator.generate()).thenReturn("WO-2026-NEWJOB2");
        when(workOrderRepository.findByCode(any())).thenReturn(Optional.empty());
        when(workOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new WorkOrderCreateRequest("Critical outage", null, 1L, 1L, Priority.CRITICAL, null, null);
        var response = workOrderService.create(request, authentication);

        // SlaProperties in this test is configured with criticalHours=4, so the auto-computed
        // due date should land roughly 4 hours out rather than being left null.
        assertThat(response.dueAt()).isNotNull();
        assertThat(response.dueAt()).isAfter(Instant.now().plusSeconds(3 * 3600));
        assertThat(response.dueAt()).isBefore(Instant.now().plusSeconds(5 * 3600));
    }

    // --- F3.3: editable while open, immutable once closed/cancelled ---

    @Test
    void cannotEditAClosedWorkOrder() {
        workOrder.setStatus(WorkOrderStatus.CLOSED);

        var request = new WorkOrderUpdateRequest("New title", "New description", Priority.HIGH, null);
        assertThatThrownBy(() -> workOrderService.update(10L, request, authentication))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void canEditAnOpenWorkOrder() {
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);

        var request = new WorkOrderUpdateRequest("Updated title", "Updated description", Priority.HIGH, null);
        var response = workOrderService.update(10L, request, authentication);

        assertThat(response.title()).isEqualTo("Updated title");
        assertThat(response.priority()).isEqualTo(Priority.HIGH);
    }
}
