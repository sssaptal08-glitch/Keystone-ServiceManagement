package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.*;
import com.KEYSTONE.ServiceManagement.dto.request.*;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.dto.response.WorkOrderResponse;
import com.KEYSTONE.ServiceManagement.dto.response.WorkOrderStatusHistoryResponse;
import com.KEYSTONE.ServiceManagement.exception.InsufficientStockException;
import com.KEYSTONE.ServiceManagement.exception.InvalidStateTransitionException;
import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import com.KEYSTONE.ServiceManagement.repository.*;
import com.KEYSTONE.ServiceManagement.security.SecurityUser;
import com.KEYSTONE.ServiceManagement.util.WorkOrderCodeGenerator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core work order business logic: the governed status state machine, dispatch/assignment,
 * parts/time logging, and — critically — object-level authorization.
 *
 * Role checks at the controller layer (@PreAuthorize) only verify WHAT a role may generally do.
 * They cannot verify WHOSE data is being touched. That check happens here:
 *   - a TECHNICIAN may only act on (or view) work orders assigned to them
 *   - a CUSTOMER may only view work orders belonging to their own organisation, and may only
 *     ever create a request against their own organisation's sites
 *   - only a MANAGER may close a work order (CLOSED is a financial/audit sign-off step)
 * Every one of these is re-checked here regardless of what the client requests, because the
 * brief explicitly calls out "assume a determined user will call your API directly."
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final PartRepository partRepository;
    private final PartUsageRepository partUsageRepository;
    private final TimeLogRepository timeLogRepository;
    private final WorkOrderCodeGenerator codeGenerator;
    private final WorkOrderEventBroadcaster eventBroadcaster;
    private final NotificationService notificationService;
    private final SlaProperties slaProperties;

    /** Legal status transitions. Enforced server-side regardless of what the client sends. */
    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS = new EnumMap<>(WorkOrderStatus.class);
    static {
        TRANSITIONS.put(WorkOrderStatus.NEW, EnumSet.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CANCELLED));
        TRANSITIONS.put(WorkOrderStatus.ASSIGNED, EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED, WorkOrderStatus.NEW));
        TRANSITIONS.put(WorkOrderStatus.IN_PROGRESS, EnumSet.of(WorkOrderStatus.ON_HOLD, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED));
        TRANSITIONS.put(WorkOrderStatus.ON_HOLD, EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED));
        TRANSITIONS.put(WorkOrderStatus.COMPLETED, EnumSet.of(WorkOrderStatus.CLOSED, WorkOrderStatus.IN_PROGRESS));
        TRANSITIONS.put(WorkOrderStatus.CLOSED, EnumSet.noneOf(WorkOrderStatus.class));
        TRANSITIONS.put(WorkOrderStatus.CANCELLED, EnumSet.noneOf(WorkOrderStatus.class));
    }

    private static final Set<WorkOrderStatus> IMMUTABLE_STATUSES =
            Set.of(WorkOrderStatus.CLOSED, WorkOrderStatus.CANCELLED);

    public PageResponse<WorkOrderResponse> findAll(WorkOrderStatus status, Long technicianId, Long customerId,
                                                    String search, Pageable pageable, Authentication authentication) {
        User caller = currentUser(authentication);

        // Object-level scoping: a customer can only ever see their own organisation's work orders,
        // and a technician can only ever see work orders assigned to them — no matter what the
        // client passes as query parameters. Client-supplied scoping params are honoured only for
        // dispatchers/managers, who are allowed to look at anyone's work.
        Long effectiveCustomerId = customerId;
        Long effectiveTechnicianId = technicianId;
        if (caller.getRole() == Role.CUSTOMER) {
            effectiveCustomerId = caller.getCustomer() != null ? caller.getCustomer().getId() : -1L;
        } else if (caller.getRole() == Role.TECHNICIAN) {
            effectiveTechnicianId = caller.getId();
        }

        Specification<WorkOrder> spec = buildSpecification(status, effectiveTechnicianId, effectiveCustomerId, search);
        Page<WorkOrder> page = workOrderRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(WorkOrderResponse::from));
    }

    public WorkOrderResponse findById(Long id, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        assertCanView(workOrder, authentication);
        return WorkOrderResponse.from(workOrder);
    }

    public List<WorkOrderStatusHistoryResponse> history(Long id, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        assertCanView(workOrder, authentication);
        return historyRepository.findByWorkOrderIdOrderByChangedAtAsc(id).stream()
                .map(WorkOrderStatusHistoryResponse::from)
                .toList();
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderCreateRequest request, Authentication authentication) {
        User createdBy = currentUser(authentication);
        boolean isCustomerRequest = createdBy.getRole() == Role.CUSTOMER;

        // F9.1/F9.4: a customer can raise a request for one of their own sites, and it enters the
        // same pipeline as a dispatcher-created work order. We never trust a customer-supplied
        // customerId — it's always forced to their own organisation server-side.
        Long effectiveCustomerId = isCustomerRequest
                ? (createdBy.getCustomer() != null ? createdBy.getCustomer().getId() : -1L)
                : request.customerId();

        if (effectiveCustomerId == null) {
            throw new IllegalArgumentException("customerId is required");
        }

        Customer customer = customerRepository.findById(effectiveCustomerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + effectiveCustomerId));
        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> new NotFoundException("Site not found: " + request.siteId()));

        if (!site.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Site does not belong to the specified customer");
        }

        // A customer raising their own request can't pre-assign a technician — that's dispatch's job.
        User technician = null;
        if (!isCustomerRequest && request.assignedTechnicianId() != null) {
            technician = getTechnicianOrThrow(request.assignedTechnicianId());
        }

        Instant now = Instant.now();
        Instant dueAt = request.dueAt() != null ? request.dueAt() : slaProperties.defaultDueDate(request.priority(), now);

        WorkOrder workOrder = WorkOrder.builder()
                .code(uniqueCode())
                .title(request.title())
                .description(request.description())
                .customer(customer)
                .site(site)
                .createdBy(createdBy)
                .assignedTechnician(technician)
                .status(technician != null ? WorkOrderStatus.ASSIGNED : WorkOrderStatus.NEW)
                .priority(request.priority())
                .dueAt(dueAt)
                .build();

        workOrder = workOrderRepository.save(workOrder);
        recordHistory(workOrder, null, workOrder.getStatus(),
                createdBy, isCustomerRequest ? "Request raised by customer" : "Work order created");
        WorkOrderResponse response = WorkOrderResponse.from(workOrder);
        eventBroadcaster.broadcast("CREATED", response);
        return response;
    }

    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderUpdateRequest request, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        if (IMMUTABLE_STATUSES.contains(workOrder.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Work order " + workOrder.getCode() + " is " + workOrder.getStatus() + " and can no longer be edited");
        }

        workOrder.setTitle(request.title());
        workOrder.setDescription(request.description());
        workOrder.setPriority(request.priority());
        if (request.dueAt() != null) {
            workOrder.setDueAt(request.dueAt());
        }

        WorkOrderResponse response = WorkOrderResponse.from(workOrder);
        eventBroadcaster.broadcast("UPDATED", response);
        return response;
    }

    @Transactional
    public WorkOrderResponse assign(Long id, WorkOrderAssignRequest request, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        User technician = getTechnicianOrThrow(request.technicianId());
        User actor = currentUser(authentication);

        WorkOrderStatus previous = workOrder.getStatus();
        workOrder.setAssignedTechnician(technician);
        if (previous == WorkOrderStatus.NEW) {
            transition(workOrder, WorkOrderStatus.ASSIGNED, actor, "Assigned to " + technician.getName());
        }
        notificationService.notifyUser(technician, NotificationType.ASSIGNMENT,
                "You've been assigned " + workOrder.getCode() + ": \"" + workOrder.getTitle() + "\"", workOrder);
        WorkOrderResponse response = WorkOrderResponse.from(workOrder);
        eventBroadcaster.broadcast("ASSIGNED", response);
        return response;
    }

    @Transactional
    public WorkOrderResponse changeStatus(Long id, WorkOrderStatusChangeRequest request, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        User actor = currentUser(authentication);

        assertCanAct(workOrder, actor);
        if (request.status() == WorkOrderStatus.CLOSED && actor.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only a manager can close a work order");
        }

        transition(workOrder, request.status(), actor, request.note());
        WorkOrderResponse response = WorkOrderResponse.from(workOrder);
        eventBroadcaster.broadcast("STATUS_CHANGED", response);
        return response;
    }

    @Transactional
    public WorkOrderResponse addPartUsage(Long id, PartUsageRequest request, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        User actor = currentUser(authentication);
        assertCanAct(workOrder, actor);

        Part part = partRepository.findById(request.partId())
                .orElseThrow(() -> new NotFoundException("Part not found: " + request.partId()));

        if (part.getQuantityOnHand() < request.quantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for part " + part.getSku() + ": have " + part.getQuantityOnHand()
                            + ", requested " + request.quantity());
        }

        part.setQuantityOnHand(part.getQuantityOnHand() - request.quantity());

        PartUsage usage = PartUsage.builder()
                .workOrder(workOrder)
                .part(part)
                .quantity(request.quantity())
                .build();
        partUsageRepository.save(usage);

        return WorkOrderResponse.from(workOrder);
    }

    @Transactional
    public WorkOrderResponse clockIn(Long id, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        User technician = currentUser(authentication);
        assertCanAct(workOrder, technician);

        timeLogRepository.findFirstByWorkOrderIdAndTechnicianIdAndEndedAtIsNull(id, technician.getId())
                .ifPresent(t -> { throw new InvalidStateTransitionException("Already clocked in on this work order"); });

        TimeLog log = TimeLog.builder()
                .workOrder(workOrder)
                .technician(technician)
                .startedAt(Instant.now())
                .build();
        timeLogRepository.save(log);
        return WorkOrderResponse.from(workOrder);
    }

    @Transactional
    public WorkOrderResponse clockOut(Long id, ClockOutRequest request, Authentication authentication) {
        WorkOrder workOrder = getOrThrow(id);
        User technician = currentUser(authentication);
        assertCanAct(workOrder, technician);

        TimeLog log = timeLogRepository.findFirstByWorkOrderIdAndTechnicianIdAndEndedAtIsNull(id, technician.getId())
                .orElseThrow(() -> new InvalidStateTransitionException("Not currently clocked in on this work order"));

        Instant now = Instant.now();
        log.setEndedAt(now);
        log.setMinutesLogged((int) java.time.Duration.between(log.getStartedAt(), now).toMinutes());
        if (request != null && StringUtils.hasText(request.note())) {
            log.setNote(request.note());
        }
        return WorkOrderResponse.from(workOrder);
    }

    // --- object-level authorization helpers ---

    /**
     * Dispatchers and managers can view any work order. A customer may only view work orders
     * belonging to their own organisation. A technician may only view work orders assigned to
     * them. Anything else throws AccessDeniedException (mapped to HTTP 403).
     */
    private void assertCanView(WorkOrder workOrder, Authentication authentication) {
        User caller = currentUser(authentication);
        switch (caller.getRole()) {
            case MANAGER, DISPATCHER -> { /* unrestricted */ }
            case CUSTOMER -> {
                if (caller.getCustomer() == null || !workOrder.getCustomer().getId().equals(caller.getCustomer().getId())) {
                    throw new AccessDeniedException("You may only view work orders for your own organisation");
                }
            }
            case TECHNICIAN -> {
                if (workOrder.getAssignedTechnician() == null || !workOrder.getAssignedTechnician().getId().equals(caller.getId())) {
                    throw new AccessDeniedException("You may only view work orders assigned to you");
                }
            }
        }
    }

    /**
     * Stricter than assertCanView: governs actions (status change, part usage, clock in/out).
     * Dispatchers/managers can act on anything; a technician can only act on their own assignment.
     * Customers can never reach this method (no controller route permits it).
     */
    private void assertCanAct(WorkOrder workOrder, User caller) {
        if (caller.getRole() == Role.TECHNICIAN
                && (workOrder.getAssignedTechnician() == null || !workOrder.getAssignedTechnician().getId().equals(caller.getId()))) {
            throw new AccessDeniedException("You may only act on work orders assigned to you");
        }
    }

    // --- query building ---

    private Specification<WorkOrder> buildSpecification(WorkOrderStatus status, Long technicianId, Long customerId, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (technicianId != null) {
                predicates.add(cb.equal(root.get("assignedTechnician").get("id"), technicianId));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("code")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // --- other internal helpers ---

    private void transition(WorkOrder workOrder, WorkOrderStatus target, User actor, String note) {
        WorkOrderStatus current = workOrder.getStatus();
        if (current == target) {
            return;
        }
        Set<WorkOrderStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidStateTransitionException(
                    "Cannot move work order from " + current + " to " + target);
        }

        workOrder.setStatus(target);
        Instant now = Instant.now();
        switch (target) {
            case IN_PROGRESS -> { if (workOrder.getStartedAt() == null) workOrder.setStartedAt(now); }
            case COMPLETED -> workOrder.setCompletedAt(now);
            case CLOSED -> workOrder.setClosedAt(now);
            default -> { /* no timestamp side-effect */ }
        }

        recordHistory(workOrder, current, target, actor, note);
    }

    private void recordHistory(WorkOrder workOrder, WorkOrderStatus from, WorkOrderStatus to, User actor, String note) {
        WorkOrderStatusHistory history = WorkOrderStatusHistory.builder()
                .workOrder(workOrder)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(actor)
                .note(note)
                .build();
        historyRepository.save(history);
    }

    private String uniqueCode() {
        String code;
        do {
            code = codeGenerator.generate();
        } while (workOrderRepository.findByCode(code).isPresent());
        return code;
    }

    private User getTechnicianOrThrow(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Technician not found: " + id));
        if (user.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("User " + id + " is not a technician");
        }
        return user;
    }

    private WorkOrder getOrThrow(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));
    }

    private User currentUser(Authentication authentication) {
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        return principal.getUser();
    }
}
