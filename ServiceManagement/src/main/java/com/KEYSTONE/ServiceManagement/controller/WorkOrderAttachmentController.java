package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.domain.WorkOrderAttachment;
import com.KEYSTONE.ServiceManagement.dto.response.AttachmentResponse;
import com.KEYSTONE.ServiceManagement.service.WorkOrderAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders/{workOrderId}/attachments")
@RequiredArgsConstructor
public class WorkOrderAttachmentController {

    private final WorkOrderAttachmentService attachmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN','CUSTOMER')")
    public List<AttachmentResponse> list(@PathVariable Long workOrderId, Authentication authentication) {
        return attachmentService.findByWorkOrder(workOrderId, authentication);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<AttachmentResponse> upload(@PathVariable Long workOrderId,
                                                      @RequestParam("file") MultipartFile file,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(workOrderId, file, authentication));
    }

    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN','CUSTOMER')")
    public ResponseEntity<Resource> download(@PathVariable Long workOrderId, @PathVariable Long attachmentId,
                                              Authentication authentication) {
        WorkOrderAttachment attachment = attachmentService.getOrThrow(workOrderId, attachmentId, authentication);
        Resource resource = attachmentService.loadFile(attachment);
        String contentType = attachment.getContentType() != null ? attachment.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long workOrderId, @PathVariable Long attachmentId,
                                        Authentication authentication) {
        attachmentService.delete(workOrderId, attachmentId, authentication);
        return ResponseEntity.noContent().build();
    }
}
