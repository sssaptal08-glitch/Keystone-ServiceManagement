-- Project KEYSTONE schema (MySQL 8+)

CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    CONSTRAINT fk_sites_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    customer_id BIGINT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE work_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    customer_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    assigned_technician_id BIGINT NULL,
    created_by_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    due_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    resolution_notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_work_orders_code UNIQUE (code),
    CONSTRAINT fk_wo_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_wo_site FOREIGN KEY (site_id) REFERENCES sites (id),
    CONSTRAINT fk_wo_technician FOREIGN KEY (assigned_technician_id) REFERENCES users (id),
    CONSTRAINT fk_wo_created_by FOREIGN KEY (created_by_id) REFERENCES users (id),
    INDEX idx_wo_status (status),
    INDEX idx_wo_technician (assigned_technician_id),
    INDEX idx_wo_customer (customer_id),
    INDEX idx_wo_due_at (due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE work_order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    changed_by_id BIGINT NOT NULL,
    note TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_wo FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE parts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    unit_cost DECIMAL(10,2) NOT NULL,
    quantity_on_hand INT NOT NULL DEFAULT 0,
    reorder_threshold INT NOT NULL DEFAULT 5,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_parts_sku UNIQUE (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE part_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_usage_wo FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_usage_part FOREIGN KEY (part_id) REFERENCES parts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE time_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NULL,
    minutes_logged INT NULL,
    CONSTRAINT fk_timelog_wo FOREIGN KEY (work_order_id) REFERENCES work_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_timelog_user FOREIGN KEY (technician_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
