package com.KEYSTONE.ServiceManagement.repository;

import com.KEYSTONE.ServiceManagement.domain.WorkOrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderAttachmentRepository extends JpaRepository<WorkOrderAttachment, Long> {
    List<WorkOrderAttachment> findByWorkOrderIdOrderByUploadedAtDesc(Long workOrderId);
    Optional<WorkOrderAttachment> findByStoredFilename(String storedFilename);
}
