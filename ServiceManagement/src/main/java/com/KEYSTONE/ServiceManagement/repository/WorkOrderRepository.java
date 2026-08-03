package com.KEYSTONE.ServiceManagement.repository;

import com.KEYSTONE.ServiceManagement.domain.Priority;
import com.KEYSTONE.ServiceManagement.domain.WorkOrder;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {

    Optional<WorkOrder> findByCode(String code);

    List<WorkOrder> findByAssignedTechnicianId(Long technicianId);

    List<WorkOrder> findByCustomerId(Long customerId);

    List<WorkOrder> findByStatusInAndDueAtBefore(List<WorkOrderStatus> statuses, Instant threshold);

    long countByStatus(WorkOrderStatus status);

    long countByPriorityAndStatusIn(Priority priority, List<WorkOrderStatus> statuses);

    long countByAssignedTechnicianIdAndStatusIn(Long technicianId, List<WorkOrderStatus> statuses);
}
