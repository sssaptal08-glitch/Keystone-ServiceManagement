package com.KEYSTONE.ServiceManagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ServiceManagementApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context (security, JPA, scheduling, Flyway-disabled H2 schema) wires up correctly.
    }

}
