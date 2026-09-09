# Project KEYSTONE — ServiceManagement Platform

A full-stack field service management application: work order lifecycle (create → assign →
in progress → on hold → completed → closed), technician dispatch, parts/inventory tracking,
SLA due-date monitoring, and role-based dashboards for managers, dispatchers, technicians, and
customers.

## Stack

| Layer     | Technology                                              |
|-----------|----------------------------------------------------------|
| Backend   | Java 21, Spring Boot 3.3.4, Spring Security (JWT), Spring Data JPA, Flyway, WebSocket/STOMP |
| Database  | MySQL 8+                                                  |
| Frontend  | React 18 + TypeScript, Vite, React Router, Axios, Recharts, lucide-react |
| Real-time | STOMP over WebSocket (SockJS fallback)                    |
| Testing   | JUnit 5 + Mockito (unit), Testcontainers + real MySQL 8 (integration) |
| CI/CD     | GitHub Actions (backend + frontend build & test on every push) |
| Build     | Maven (wrapper included), npm                             |

> **Fixed in this revision:** the original generated `pom.xml` pointed at a placeholder
> `spring-boot-starter-parent:4.1.0` plus several test-starter artifact IDs
> (`spring-boot-starter-data-jpa-test`, `-security-test`, `-validation-test`, `-webmvc-test`,
> and `spring-boot-starter-webmvc` instead of `spring-boot-starter-web`) that are **not real,
> published Maven artifacts** — that was the build error. The `pom.xml` now pins the well-known,
> verified **Spring Boot 3.3.4** release with standard artifact names (`spring-boot-starter-web`,
> `spring-boot-starter-test`, etc.), which resolves cleanly from Maven Central.

## Quick start (Docker — recommended, one command)

> **Deploying for real (public URL)?** See [`DEPLOYMENT.md`](./DEPLOYMENT.md) for a step-by-step
> guide to deploying this to Railway's free tier (MySQL-compatible, Dockerfiles already included).
> Everything below covers running it locally.

This spins up MySQL, the backend, and the frontend together:

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

- Frontend: https://keystone-service-management.vercel.app/login
- Backend API: https://keystone-servicemanagement-production.up.railway.app (Swagger UI at `/swagger-ui.html`)
- MySQL: localhost:3306 (`keystone` / `keystone_user` / `keystone_pass`)

Flyway creates the schema and loads demo seed data automatically on first backend boot. Give
the backend ~30-60s to become healthy before the frontend's first API calls succeed (the
frontend container has no hard dependency wait, so an early refresh may briefly show an error —
just reload).

To stop everything: `docker compose -f docker-compose.prod.yml down` (add `-v` to also wipe the
MySQL volume and start fresh next time).

## Local development (without Docker for the app code)

1. **Start just MySQL** via Docker:
   ```bash
   docker compose up -d mysql
   ```

2. **Run the backend**:
   ```bash
   ./mvnw spring-boot:run
   ```
   API on `http://localhost:8080`, Swagger UI at `http://localhost:8080/swagger-ui.html`.

   Override connection settings via env vars instead of editing `application.properties`:
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/keystone?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   export DB_USERNAME=keystone_user
   export DB_PASSWORD=keystone_pass
   export JWT_SECRET="change-me-to-a-long-random-string-in-production"
   ```

3. **Run the frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Dev server on `http://localhost:5173`, proxying `/api` to `localhost:8080` (see
   `vite.config.ts`).

4. **Run backend tests**:
   ```bash
   ./mvnw test
   ```
   This runs both the plain Mockito unit tests (H2, instant, no external services) and
   `WorkOrderIntegrationTest`, which needs a local Docker daemon running — Testcontainers uses
   it to spin up a real, disposable MySQL 8 container for the duration of the test.

## Running in IntelliJ IDEA

1. `File → Open` the `ServiceManagement` folder (the one with `pom.xml`) — IntelliJ imports it
   as a Maven project automatically.
2. `File → Project Structure → Project` → set Project SDK to **Java 21**.
3. Start MySQL: open the built-in Terminal (`Alt+F12`) and run `docker compose up -d mysql`.
4. `Run → Edit Configurations → + → Spring Boot`, main class
   `com.KEYSTONE.ServiceManagement.ServiceManagementApplication`, then run it.
5. For the frontend: open a Terminal in `frontend/`, run `npm install && npm run dev`, or add an
   npm Run Configuration (`Run → Edit Configurations → + → npm`, script `dev`).
6. IntelliJ's Database tool window (`View → Tool Windows → Database`) can connect to
   `localhost:3306` / db `keystone` / `keystone_user` / `keystone_pass` to inspect what Flyway
   created.

### Demo accounts (seeded by Flyway)

All seeded accounts use the password `Password123!`.

| Email                          | Role       |
|---------------------------------|------------|
| manager@keystone.example         | MANAGER    |
| dispatcher@keystone.example       | DISPATCHER |
| tom.tech@keystone.example          | TECHNICIAN |
| nina.tech@keystone.example          | TECHNICIAN |
| carla@acme-mfg.example                | CUSTOMER   |

## Project layout

```
ServiceManagement/
├── pom.xml                      # Backend build (Spring Boot 3.3.4, Java 21)
├── mvnw, mvnw.cmd, .mvn/          # Maven wrapper
├── Dockerfile                     # Backend production image (multi-stage Maven build)
├── docker-compose.yml               # MySQL only, for local dev against ./mvnw / npm run dev
├── docker-compose.prod.yml           # Full stack: MySQL + backend + frontend (nginx)
├── src/
│   ├── main/java/com/KEYSTONE/ServiceManagement/
│   │   ├── domain/               # JPA entities
│   │   ├── repository/           # Spring Data repositories
│   │   ├── dto/request|response/  # API request/response records
│   │   ├── service/                # Business logic (state machine, dispatch, inventory, reports)
│   │   ├── controller/              # REST controllers
│   │   ├── security/                 # JWT auth filter, JwtService, UserDetailsService
│   │   ├── config/                    # Security, CORS, OpenAPI config
│   │   └── exception/                  # Global exception handling
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── db/migration/                # Flyway SQL migrations (schema + seed data)
│   └── test/                              # Unit tests (Mockito, no Spring context) + context test
└── frontend/                                # React + TypeScript SPA (Vite)
    ├── Dockerfile                             # Production image (Node build → nginx)
    ├── nginx.conf                              # SPA routing + /api reverse proxy to backend
    └── src/
        ├── api/                                 # Axios client + typed service calls
        ├── context/                              # Auth context (JWT storage)
        ├── components/                            # Layout, route guards, badges
        └── pages/                                  # Login, Dashboard, Work Orders, Customers, Inventory
```

## What's new in this revision

- **Live updates over WebSocket** — creating a work order, assigning a technician, or changing
  status broadcasts a STOMP event on `/topic/work-orders`. Every connected dashboard/job board
  picks it up instantly (see the "Live"/"Offline" pill on the dashboard). Falls back gracefully
  to plain REST if the socket can't connect — nothing breaks without it.
- **Photo/file attachments on work orders** — technicians can attach photos or documents
  (drag-and-drop or click to browse, 15MB limit) directly on a work order's detail page.
  Stored on local disk behind `FileStorageService` (swappable for S3/GCS later), tracked in a
  new `work_order_attachments` table, downloadable/deletable with the same RBAC rules as
  everything else.
- **Real charts on the dashboard** — status breakdown and open-by-priority are now an actual bar
  chart and donut chart (Recharts), not just a number list.
- **Dark mode** — toggle in the top bar, persisted to `localStorage`, respects system preference
  on first visit.
- **Testcontainers integration tests** — `WorkOrderIntegrationTest` boots the full app against a
  real, disposable MySQL 8 container (not H2) and drives the actual REST API: login, create,
  assign, illegal-transition rejection, legal transition, audit history, and an RBAC check.
  Requires a local Docker daemon to run.
- **CI pipeline** (`.github/workflows/ci.yml`) — every push/PR runs the full backend test suite
  (unit + Testcontainers integration) and builds the jar, plus a separate job that type-checks
  and builds the frontend. GitHub's hosted runners already have Docker installed, so the
  Testcontainers job works with no extra setup.
- **Architecture documentation** — see [`ARCHITECTURE.md`](./ARCHITECTURE.md) for system,
  sequence, state-machine, and ER diagrams (Mermaid, renders directly on GitHub).

## Brief-compliance fixes (gap analysis against the official spec)

A close read of the client brief (Project KEYSTONE — Java Full-Stack Engineering Brief) surfaced
several gaps between what it grades and what the app actually enforced. All are now fixed:

### Round 2 — customer self-service, board view, SLA automation

- **F9 customer self-service requests** — the customer portal was previously view-only. A
  `CUSTOMER`-role user can now actually raise a request (`POST /api/work-orders`); the backend
  always forces `customerId` to their own organisation server-side regardless of what the client
  sends, and disallows pre-assigning a technician (that stays dispatch's job). Requests enter the
  exact same pipeline/state machine as a dispatcher-created work order (F9.4).
- **F4.4 Kanban board** — `/work-orders` now has a List/Board toggle. The board shows one column
  per open status (NEW → ASSIGNED → IN_PROGRESS → ON_HOLD → COMPLETED; closed/cancelled work
  lives in the list view and Activity log instead) with drag-and-drop cards. Dropping a card
  calls the same `PATCH /status` endpoint as everywhere else — the server's state machine is the
  single source of truth, so an illegal drag (e.g. NEW straight to IN_PROGRESS) is rejected with
  the same 409 and surfaced as an error, not silently allowed client-side.
- **F7.1 SLA due date now actually computed from priority** — previously `app.sla.critical-hours`
  etc. were configured but never read; a dispatcher had to manually pick a due date every time.
  `SlaProperties` now supplies a default (`now + N hours` based on priority) whenever a work
  order is created without an explicit `dueAt`.
- **F7.2 "at risk" warnings, not just breach** — `SlaMonitorService` now has two tiers: **at
  risk** (due within `app.sla.at-risk-hours`, not yet overdue) and **breached** (past due). Both
  generate a deduplicated, persisted, live-broadcast notification to managers — at-risk work
  orders get an amber warning before they become a red breach, instead of managers only finding
  out after the fact.
- **F8.1 SLA compliance %** — the dashboard now shows a donut gauge (mirroring the brief's own
  illustrative Figure 6: "88% SLA met") computed as the share of COMPLETED/CLOSED work orders
  that finished at or before their due date. Shows "nothing to measure yet" rather than a
  misleading 0%/100% when no work has been resolved yet.
- **F6.3 parts cost / labour time roll-ups** — `WorkOrderResponse` now includes
  `totalPartsCost` (sum of `quantity × unitCost` across all logged parts) and
  `totalMinutesLogged` (sum of all completed time-log sessions), shown on the work order detail
  page instead of requiring a manual tally.
- **F6.2 time entries can carry a note** — `TimeLog` gained a `note` column; a technician can
  describe what they did when they clock out (optional, matches "Time entries record minutes and
  an optional note").
- **F3.3 editable while open, immutable once closed/cancelled** — added
  `PUT /api/work-orders/{id}` (title/description/priority/due date), restricted to
  dispatcher/manager, and rejected with 409 once a work order reaches `CLOSED` or `CANCELLED`.

### Round 1

- **Object-level authorization** — role checks alone (`@PreAuthorize`) only verify *what* a role
  may generally do, not *whose* data it's touching. `WorkOrderService` now enforces:
  - a **technician** can only view or act on (status change, part usage, clock in/out) work
    orders assigned to them — acting on someone else's job returns HTTP 403;
  - a **customer** can only view work orders belonging to their own organisation, regardless of
    what `customerId` a request tries to pass;
  - only a **manager** can transition a work order to `CLOSED` (dispatchers/technicians get 403).

  Attachment endpoints reuse this same check (`WorkOrderAttachmentService` delegates to
  `WorkOrderService.findById`) rather than re-implementing it, so there's one source of truth.
  Covered by six new tests in `WorkOrderServiceTest` (`onlyManagerCanCloseAWorkOrder`,
  `technicianCannotActOnAWorkOrderAssignedToSomeoneElse`, `customerCannotViewAnotherCustomersWorkOrder`, etc.)
  and by the `technicianCannotCreateWorkOrders` / `unauthenticatedRequestIsRejected` integration tests.

- **Pagination, search, and filtering** — the brief requires list endpoints to be "searchable and
  paginated" and to "never return unbounded result sets" (F2, F3, §10.1). `/api/work-orders`,
  `/api/customers`, and `/api/parts` now all accept `page`, `size`, and `search` query params and
  return a `PageResponse<T>` envelope (`content`, `page`, `totalPages`, `totalElements`). The
  frontend has a shared `<Pagination>` component and search boxes on all three list pages.

- **Dashboard breakdown by technician** — F8.3 requires "at least one view breaks results down by
  technician or site." Added a per-technician open-job-count breakdown to
  `/api/reports/dashboard`, rendered as a bar chart, sorted busiest-first so a dispatcher can spot
  overload at a glance.

- **SLA breach notifications reach managers** — F7.3 requires breaches to be "visible to managers
  **and trigger a notification**," not just logged server-side. Added a full in-app notification
  system: a `notifications` table, a bell icon with unread badge in the top bar, live push over
  the existing WebSocket connection, and persistence so a manager sees what they missed since
  they were last online. `SlaMonitorService` now creates one notification per breach (deduplicated
  so a stuck job doesn't spam) instead of only writing a log line. Technicians also get an
  `ASSIGNMENT` notification the moment a dispatcher assigns them a job.

- **Technician workload indicator** — the brief's own optional stretch list (§13.1) suggests
  "technician availability/skills so dispatch can suggest the best match." Implemented the
  lightweight version: `/api/users/technicians` now returns each technician's current open-job
  count, shown next to their name in every assignment dropdown.

- **Activity/audit log viewer** — also from §13.1's stretch list. `/api/reports/activity` surfaces
  the most recent status changes across every work order (built from the existing append-only
  `work_order_status_history` table — no new audit mechanism needed), with a dedicated Activity
  page in the nav for managers/dispatchers.

## Database: MySQL vs. the brief's PostgreSQL

The official brief specifies PostgreSQL as a fixed part of the stack. This project uses **MySQL
8+** instead, per an explicit decision to keep MySQL and confirm the deviation with a mentor (the
brief allows substituting technologies with mentor sign-off — see §11, "Allowed alternatives").
If mentor sign-off isn't obtained, migrating back to PostgreSQL means: swapping
`mysql-connector-j` for `org.postgresql:postgresql` in `pom.xml`, changing
`spring.jpa.properties.hibernate.dialect` to `PostgreSQLDialect`, replacing `AUTO_INCREMENT` /
`TINYINT(1)` / backtick-free MySQL syntax in the Flyway scripts with Postgres equivalents
(`GENERATED ALWAYS AS IDENTITY` / `BOOLEAN`), and swapping the `mysql:8.0.36` image for
`postgres:16` in `docker-compose*.yml`. The application code itself (JPA entities, repositories,
services) needs no changes — that's the point of using Spring Data JPA over raw SQL.

## Core domain model & business rules

- **Work order lifecycle** is enforced server-side as a strict state machine (see
  `WorkOrderService`): `NEW → ASSIGNED → IN_PROGRESS → (ON_HOLD ↔ IN_PROGRESS) → COMPLETED →
  CLOSED`, with `CANCELLED` reachable from any open state. Illegal transitions are rejected with
  HTTP 409 regardless of what the client requests. Covered by `WorkOrderServiceTest`.
- **Every status change is audited** in `work_order_status_history` with who made the change,
  when, and an optional note.
- **Parts usage** decrements on-hand inventory transactionally and rejects the request with
  HTTP 409 if stock is insufficient — no negative inventory is possible.
- **SLA monitoring**: a scheduled job (`SlaMonitorService`) periodically scans for open work
  orders past their due date; the API also flags overdue work orders inline (`overdue: true`)
  on every response.
- **Role-based access control** (`DISPATCHER`, `TECHNICIAN`, `MANAGER`, `CUSTOMER`) is enforced
  with method-level `@PreAuthorize` checks on every controller endpoint, backed by JWT bearer
  tokens issued at `/api/auth/login`.

## Production deployment notes

- **Change the JWT secret** before deploying anywhere real: set the `JWT_SECRET` env var to a
  long random string (32+ bytes). The default in `application.properties` is a placeholder.
- **Change the MySQL passwords** in `docker-compose.prod.yml` (`MYSQL_PASSWORD`,
  `MYSQL_ROOT_PASSWORD`) before exposing this beyond your local machine.
- **CORS**: `CORS_ALLOWED_ORIGINS` on the backend must include whatever origin your frontend is
  actually served from. `docker-compose.prod.yml` sets it to `http://localhost:8081` to match the
  frontend container's published port — update it if you deploy behind a real domain/HTTPS.
- The frontend's Dockerfile builds with `VITE_API_BASE_URL=/api` and relies on the nginx reverse
  proxy (`frontend/nginx.conf`) to forward `/api/*` and `/ws/*` to the `backend` service — this
  avoids CORS entirely in the Docker deployment since both are served from the same origin.
- **Uploaded attachments persist in a named Docker volume** (`keystone_uploads`, mounted at
  `/app/uploads` in the backend container) so they survive container restarts/rebuilds. They do
  **not** survive `docker compose down -v`, same as the MySQL data volume.
- The WebSocket endpoint (`/ws`) currently allows all origins (`setAllowedOriginPatterns("*")`)
  for demo convenience — lock this down to your real frontend origin before deploying publicly.
