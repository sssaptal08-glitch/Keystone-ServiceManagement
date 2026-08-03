package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.User;
import com.KEYSTONE.ServiceManagement.domain.WorkOrder;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderAttachment;
import com.KEYSTONE.ServiceManagement.dto.response.AttachmentResponse;
import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import com.KEYSTONE.ServiceManagement.repository.WorkOrderAttachmentRepository;
import com.KEYSTONE.ServiceManagement.repository.WorkOrderRepository;
import com.KEYSTONE.ServiceManagement.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Attachment access reuses WorkOrderService's object-level authorization (findById) rather than
 * duplicating the "customer sees only their own org, technician sees only their own jobs" rule
 * a second time — one source of truth for who can see a given work order at all.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderAttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15MB

    private final WorkOrderAttachmentRepository attachmentRepository;
    private final WorkOrderRepository workOrderRepository;
    private final FileStorageService fileStorageService;
    private final WorkOrderService workOrderService;

    public List<AttachmentResponse> findByWorkOrder(Long workOrderId, Authentication authentication) {
        workOrderService.findById(workOrderId, authentication); // throws if caller can't view this work order
        return attachmentRepository.findByWorkOrderIdOrderByUploadedAtDesc(workOrderId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional
    public AttachmentResponse upload(Long workOrderId, MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was provided");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the 15MB upload limit");
        }

        workOrderService.findById(workOrderId, authentication); // enforces technician-owns-job rule too
        WorkOrder workOrder = getWorkOrderOrThrow(workOrderId);
        User uploader = ((SecurityUser) authentication.getPrincipal()).getUser();

        String storedFilename = fileStorageService.store(file);

        WorkOrderAttachment attachment = WorkOrderAttachment.builder()
                .workOrder(workOrder)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(uploader)
                .build();

        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    public WorkOrderAttachment getOrThrow(Long workOrderId, Long attachmentId, Authentication authentication) {
        workOrderService.findById(workOrderId, authentication);
        WorkOrderAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));
        if (!attachment.getWorkOrder().getId().equals(workOrderId)) {
            throw new NotFoundException("Attachment not found on this work order: " + attachmentId);
        }
        return attachment;
    }

    public Resource loadFile(WorkOrderAttachment attachment) {
        return fileStorageService.loadAsResource(attachment.getStoredFilename());
    }

    @Transactional
    public void delete(Long workOrderId, Long attachmentId, Authentication authentication) {
        WorkOrderAttachment attachment = getOrThrow(workOrderId, attachmentId, authentication);
        fileStorageService.delete(attachment.getStoredFilename());
        attachmentRepository.delete(attachment);
    }

    private WorkOrder getWorkOrderOrThrow(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));
    }
}
