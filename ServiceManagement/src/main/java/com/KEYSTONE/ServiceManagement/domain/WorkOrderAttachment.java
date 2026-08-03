package com.KEYSTONE.ServiceManagement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "work_order_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    /** Randomized filename actually used on disk, to avoid collisions and path traversal. */
    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
