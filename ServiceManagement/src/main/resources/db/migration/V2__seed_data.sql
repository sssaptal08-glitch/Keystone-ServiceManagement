-- Seed data for local development / demo.
-- Demo password for ALL seeded users: Password123!

INSERT INTO customers (id, name, contact_email, contact_phone) VALUES
    (1, 'Acme Manufacturing', 'ops@acme-mfg.example', '555-0101'),
    (2, 'Northwind Retail Group', 'facilities@northwind.example', '555-0102');

INSERT INTO sites (id, customer_id, name, address, city, state, postal_code) VALUES
    (1, 1, 'Acme Plant 1', '100 Industrial Pkwy', 'Springfield', 'IL', '62701'),
    (2, 1, 'Acme Warehouse 4', '450 Logistics Ave', 'Springfield', 'IL', '62702'),
    (3, 2, 'Northwind Store #12', '77 Market St', 'Columbus', 'OH', '43004');

-- password_hash below is bcrypt for: Password123!
INSERT INTO users (id, name, email, password_hash, role, customer_id, enabled) VALUES
    (1, 'Maria Manager', 'manager@keystone.example', '$2b$10$hVzyRUC4q.KF5peMc8xrUObxdobstRl4uUaWETaXok/DLRVxgZbXK', 'MANAGER', NULL, 1),
    (2, 'Derek Dispatcher', 'dispatcher@keystone.example', '$2b$10$hVzyRUC4q.KF5peMc8xrUObxdobstRl4uUaWETaXok/DLRVxgZbXK', 'DISPATCHER', NULL, 1),
    (3, 'Tom Technician', 'tom.tech@keystone.example', '$2b$10$hVzyRUC4q.KF5peMc8xrUObxdobstRl4uUaWETaXok/DLRVxgZbXK', 'TECHNICIAN', NULL, 1),
    (4, 'Nina Technician', 'nina.tech@keystone.example', '$2b$10$hVzyRUC4q.KF5peMc8xrUObxdobstRl4uUaWETaXok/DLRVxgZbXK', 'TECHNICIAN', NULL, 1),
    (5, 'Carla Customer', 'carla@acme-mfg.example', '$2b$10$hVzyRUC4q.KF5peMc8xrUObxdobstRl4uUaWETaXok/DLRVxgZbXK', 'CUSTOMER', 1, 1);

INSERT INTO parts (id, sku, name, unit_cost, quantity_on_hand, reorder_threshold) VALUES
    (1, 'BRG-1001', 'Sealed Ball Bearing 20mm', 8.50, 40, 10),
    (2, 'BLT-2050', 'Drive Belt 50in', 22.00, 6, 5),
    (3, 'FLT-3300', 'HVAC Air Filter 20x20', 14.75, 3, 5),
    (4, 'MOT-4400', '1/2 HP Motor', 145.00, 12, 3);

INSERT INTO work_orders (id, code, title, description, customer_id, site_id, assigned_technician_id, created_by_id,
                          status, priority, due_at, started_at, created_at, updated_at) VALUES
    (1, 'WO-2026-A1B2C', 'Conveyor belt slipping on line 3', 'Belt appears worn and slipping intermittently under load.',
        1, 1, 3, 2, 'IN_PROGRESS', 'HIGH', DATE_ADD(NOW(), INTERVAL 6 HOUR), NOW(), NOW(), NOW()),
    (2, 'WO-2026-D4E5F', 'HVAC filter replacement', 'Routine quarterly filter swap for rooftop units.',
        2, 3, NULL, 2, 'NEW', 'LOW', DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, NOW(), NOW()),
    (3, 'WO-2026-G6H7I', 'Warehouse door motor failure', 'Loading dock door motor tripped breaker, will not reset.',
        1, 2, 4, 2, 'ASSIGNED', 'CRITICAL', DATE_ADD(NOW(), INTERVAL 4 HOUR), NULL, NOW(), NOW());

INSERT INTO work_order_status_history (work_order_id, from_status, to_status, changed_by_id, note, changed_at) VALUES
    (1, NULL, 'NEW', 2, 'Work order created', NOW()),
    (1, 'NEW', 'ASSIGNED', 2, 'Assigned to Tom Technician', NOW()),
    (1, 'ASSIGNED', 'IN_PROGRESS', 3, 'Started diagnosis', NOW()),
    (2, NULL, 'NEW', 2, 'Work order created', NOW()),
    (3, NULL, 'NEW', 2, 'Work order created', NOW()),
    (3, 'NEW', 'ASSIGNED', 2, 'Assigned to Nina Technician', NOW());
