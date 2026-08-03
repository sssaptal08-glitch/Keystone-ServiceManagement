package com.KEYSTONE.ServiceManagement.repository;

import com.KEYSTONE.ServiceManagement.domain.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
    List<TimeLog> findByWorkOrderId(Long workOrderId);
    Optional<TimeLog> findFirstByWorkOrderIdAndTechnicianIdAndEndedAtIsNull(Long workOrderId, Long technicianId);
}
