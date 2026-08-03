package com.KEYSTONE.ServiceManagement.repository;

import com.KEYSTONE.ServiceManagement.domain.PartUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartUsageRepository extends JpaRepository<PartUsage, Long> {
    List<PartUsage> findByWorkOrderId(Long workOrderId);
}
