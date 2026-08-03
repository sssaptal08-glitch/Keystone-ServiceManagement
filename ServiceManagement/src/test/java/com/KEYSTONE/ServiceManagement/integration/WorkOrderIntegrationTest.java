package com.KEYSTONE.ServiceManagement.integration;

import com.KEYSTONE.ServiceManagement.dto.request.LoginRequest;
import com.KEYSTONE.ServiceManagement.dto.request.WorkOrderAssignRequest;
import com.KEYSTONE.ServiceManagement.dto.request.WorkOrderCreateRequest;
import com.KEYSTONE.ServiceManagement.dto.request.WorkOrderStatusChangeRequest;
import com.KEYSTONE.ServiceManagement.dto.response.AuthResponse;
import com.KEYSTONE.ServiceManagement.dto.response.WorkOrderResponse;
import com.KEYSTONE.ServiceManagement.domain.Priority;
import com.KEYSTONE.ServiceManagement.domain.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test that boots the full Spring context against a real MySQL 8
 * instance (via Testcontainers) instead of H2, runs the actual Flyway migrations, and drives
 * the REST API exactly the way a real client would: login → create a work order → assign a
 * technician → walk it through the status lifecycle.
 *
 * Requires a local Docker daemon to run. In CI this is provided by the GitHub Actions runner;
 * locally, `docker` must be installed and running.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkOrderIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
            .withDatabaseName("keystone")
            .withUsername("keystone_user")
            .withPassword("keystone_pass");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Keep Flyway enabled (unlike the H2 unit-test profile) so migrations run against real MySQL.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private String loginAndGetToken(String email, String password) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, password), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().token();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void fullWorkOrderLifecycleThroughRealMysql() {
        String dispatcherToken = loginAndGetToken("dispatcher@keystone.example", "Password123!");

        // Create a new work order for the seeded Acme Manufacturing / Acme Plant 1 site.
        WorkOrderCreateRequest createRequest = new WorkOrderCreateRequest(
                "Integration test: leaking valve", "Discovered during routine inspection.",
                1L, 1L, Priority.MEDIUM, Instant.now().plusSeconds(3600 * 12), null);

        ResponseEntity<WorkOrderResponse> createResponse = restTemplate.exchange(
                "/api/work-orders", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders(dispatcherToken)), WorkOrderResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        WorkOrderResponse created = createResponse.getBody();
        assertThat(created).isNotNull();
        assertThat(created.status()).isEqualTo(WorkOrderStatus.NEW);
        assertThat(created.code()).startsWith("WO-");

        Long workOrderId = created.id();

        // Assign to the seeded technician (Tom Technician, id=3) — this should auto-transition NEW -> ASSIGNED.
        ResponseEntity<WorkOrderResponse> assignResponse = restTemplate.exchange(
                "/api/work-orders/" + workOrderId + "/assign", HttpMethod.PATCH,
                new HttpEntity<>(new WorkOrderAssignRequest(3L), authHeaders(dispatcherToken)), WorkOrderResponse.class);

        assertThat(assignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assignResponse.getBody().status()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(assignResponse.getBody().assignedTechnicianName()).isEqualTo("Tom Technician");

        // Illegal transition (ASSIGNED -> COMPLETED, skipping IN_PROGRESS) must be rejected with 409.
        ResponseEntity<String> illegalResponse = restTemplate.exchange(
                "/api/work-orders/" + workOrderId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(new WorkOrderStatusChangeRequest(WorkOrderStatus.COMPLETED, null), authHeaders(dispatcherToken)),
                String.class);
        assertThat(illegalResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Legal transition ASSIGNED -> IN_PROGRESS succeeds.
        ResponseEntity<WorkOrderResponse> progressResponse = restTemplate.exchange(
                "/api/work-orders/" + workOrderId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(new WorkOrderStatusChangeRequest(WorkOrderStatus.IN_PROGRESS, "Technician on site"), authHeaders(dispatcherToken)),
                WorkOrderResponse.class);
        assertThat(progressResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(progressResponse.getBody().status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);

        // Audit history should now have 3 entries: created, assigned, in-progress.
        ResponseEntity<Object[]> historyResponse = restTemplate.exchange(
                "/api/work-orders/" + workOrderId + "/history", HttpMethod.GET,
                new HttpEntity<>(authHeaders(dispatcherToken)), Object[].class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).hasSize(3);
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/work-orders", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void technicianCannotCreateWorkOrders() {
        // RBAC check: TECHNICIAN role must not be able to create work orders (dispatcher/manager only).
        String technicianToken = loginAndGetToken("tom.tech@keystone.example", "Password123!");

        WorkOrderCreateRequest createRequest = new WorkOrderCreateRequest(
                "Should be forbidden", null, 1L, 1L, Priority.LOW, null, null);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/work-orders", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders(technicianToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
