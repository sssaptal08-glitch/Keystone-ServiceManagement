# Architecture — Project KEYSTONE

## System overview

```mermaid
flowchart TB
    subgraph Client["Browser"]
        SPA["React SPA (Vite build)"]
    end

    subgraph Edge["nginx (frontend container)"]
        Static["Static assets: HTML/JS/CSS"]
        Proxy["Reverse proxy: /api, /ws, /swagger-ui"]
    end

    subgraph App["Spring Boot backend"]
        Sec["Security filter chain (JWT)"]
        Ctrl["REST controllers"]
        WS["STOMP / WebSocket broker"]
        Svc["Service layer (state machine, RBAC, business rules)"]
        Repo["Spring Data JPA repositories"]
    end

    subgraph Data["Persistence"]
        MySQL[("MySQL 8")]
        Disk[("Local disk volume: /uploads")]
    end

    SPA -- "HTTPS" --> Static
    SPA -- "REST calls" --> Proxy
    SPA -- "STOMP over WebSocket" --> Proxy
    Proxy --> Sec
    Sec --> Ctrl
    Ctrl --> Svc
    Svc --> Repo
    Repo --> MySQL
    Ctrl -- "file upload/download" --> Disk
    Svc -- "broadcast work order events" --> WS
    WS -- "push to subscribers" --> Proxy
```

## Request flow: creating and dispatching a work order

```mermaid
sequenceDiagram
    actor Dispatcher
    participant SPA as React SPA
    participant API as Spring Boot API
    participant DB as MySQL
    participant WS as WebSocket broker
    participant Tech as Technician's browser

    Dispatcher->>SPA: Fill "New Work Order" form
    SPA->>API: POST /api/work-orders (JWT bearer)
    API->>API: Validate DTO, check role (@PreAuthorize)
    API->>DB: INSERT work_orders, work_order_status_history
    API->>WS: broadcast CREATED event on /topic/work-orders
    API-->>SPA: 201 Created + WorkOrderResponse
    WS-->>Tech: push CREATED event
    Tech->>Tech: Job board updates live, no refresh needed
```

## Work order status state machine

Enforced server-side in `WorkOrderService` — the API rejects any transition not shown below
with HTTP 409, regardless of what the client requests.

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> ASSIGNED
    NEW --> CANCELLED
    ASSIGNED --> IN_PROGRESS
    ASSIGNED --> NEW
    ASSIGNED --> CANCELLED
    IN_PROGRESS --> ON_HOLD
    IN_PROGRESS --> COMPLETED
    IN_PROGRESS --> CANCELLED
    ON_HOLD --> IN_PROGRESS
    ON_HOLD --> CANCELLED
    COMPLETED --> CLOSED
    COMPLETED --> IN_PROGRESS
    CLOSED --> [*]
    CANCELLED --> [*]
```

## Request flow: SLA breach notification

```mermaid
sequenceDiagram
    participant Scheduler as SlaMonitorService (every 5 min)
    participant DB as MySQL
    participant NS as NotificationService
    participant WS as WebSocket broker
    actor Manager

    Scheduler->>DB: find open work orders past dueAt
    loop each newly-breached work order
        Scheduler->>NS: already notified for this breach?
        NS->>DB: check notifications table
        alt not yet notified
            Scheduler->>NS: notifyRole(MANAGER, SLA_BREACH, ...)
            NS->>DB: INSERT notification (per manager)
            NS->>WS: broadcast on /topic/notifications
            WS-->>Manager: push, filtered client-side by recipientId
            Manager->>Manager: bell badge increments, item appears live
        end
    end
```

## Data model (core entities)

```mermaid
erDiagram
    CUSTOMER ||--o{ SITE : "has"
    CUSTOMER ||--o{ USER : "customer users belong to"
    SITE ||--o{ WORK_ORDER : "location of"
    CUSTOMER ||--o{ WORK_ORDER : "requested by"
    USER ||--o{ WORK_ORDER : "assigned technician"
    WORK_ORDER ||--o{ WORK_ORDER_STATUS_HISTORY : "audit trail"
    WORK_ORDER ||--o{ WORK_ORDER_ATTACHMENT : "photos/files"
    WORK_ORDER ||--o{ PART_USAGE : "parts consumed"
    WORK_ORDER ||--o{ TIME_LOG : "clock in/out"
    PART ||--o{ PART_USAGE : "used in"
    USER ||--o{ NOTIFICATION : "receives"
    WORK_ORDER ||--o{ NOTIFICATION : "concerns"
```

## Why these choices

- **Server-enforced state machine** rather than trusting the client: a technician's app or a
  buggy frontend build can never corrupt a work order into an invalid status — the same
  `TRANSITIONS` map is the single source of truth for every entry point (REST, and in the
  future, any other integration).
- **STOMP/WebSocket over polling**: dispatch boards are inherently multi-user — a dispatcher
  assigning a job should be visible to the technician immediately. Polling every few seconds
  wastes requests and still has visible lag; a broadcast on state change is both cheaper and
  faster.
- **Local disk for attachments, behind an interface (`FileStorageService`)**: keeps the Docker
  Compose stack dependency-free for local dev/grading, while the storage logic is isolated
  behind one class — swapping to S3/GCS later touches only that file.
- **Testcontainers for integration tests**: H2 is fast for pure unit/service-layer tests, but it
  is not MySQL — subtle SQL dialect differences (and Flyway migrations meant for MySQL) are only
  actually verified by testing against a real MySQL 8 container.
