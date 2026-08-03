package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.WorkOrderAttachment;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String originalFilename,
        String contentType,
        long fileSize,
        String uploadedByName,
        Instant uploadedAt,
        String downloadUrl
) {
    public static AttachmentResponse from(WorkOrderAttachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getOriginalFilename(),
                a.getContentType(),
                a.getFileSize(),
                a.getUploadedBy().getName(),
                a.getUploadedAt(),
                "/api/work-orders/" + a.getWorkOrder().getId() + "/attachments/" + a.getId() + "/download"
        );
    }
}
