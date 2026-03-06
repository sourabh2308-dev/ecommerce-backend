package com.sourabh.order_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test that verifies the Order Service Spring application context
 * loads successfully with all beans wired correctly.
 *
 * <p>Uses <strong>Testcontainers</strong> to spin up disposable PostgreSQL 15
 * and Redis 7 instances so that datasource and cache auto-configuration
 * resolve against real infrastructure.</p>
 *
 * <h3>Test-Specific Overrides</h3>
 * <ul>
 *   <li>{@code spring.kafka.listener.auto-startup=false} – prevents Kafka
 *       consumers from starting (no broker available in this test).</li>
 *   <li>{@code spring.jpa.hibernate.ddl-auto=update} – lets Hibernate create
 *       the schema in the ephemeral PostgreSQL container.</li>
 * </ul>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Disabled("Testcontainers incompatible with Docker 29+ (API v1.44 minimum)")
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
class OrderServiceApplicationTests {

    /** Disposable PostgreSQL 15 container auto-configured via {@code @ServiceConnection}. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    /** Disposable Redis 7 container exposing the default port for cache integration. */
    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    /**
     * Validates that the full application context starts without errors.
     *
     * <p>An empty test body is intentional – {@code @SpringBootTest} triggers
     * context loading, and any wiring failure causes the test to fail.</p>
     */
    @Test
    void contextLoads() {
    }

}
