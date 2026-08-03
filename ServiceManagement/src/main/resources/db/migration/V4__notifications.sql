-- In-app notifications: SLA breach alerts and assignment notices, surfaced live via WebSocket
-- and persisted so a user sees what they missed since they were last online.

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    work_order_id BIGINT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_wo FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    INDEX idx_notifications_recipient (recipient_id, created_at),
    INDEX idx_notifications_wo_type (work_order_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
