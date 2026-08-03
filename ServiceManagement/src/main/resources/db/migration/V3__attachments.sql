-- Attachments (photos/documents) that can be uploaded against a work order.

CREATE TABLE work_order_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL DEFAULT 0,
    uploaded_by_id BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attachments_stored_filename UNIQUE (stored_filename),
    CONSTRAINT fk_attachments_wo FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_attachments_user FOREIGN KEY (uploaded_by_id) REFERENCES users (id),
    INDEX idx_attachments_wo (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
