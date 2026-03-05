package com.sourabh.review_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration smoke test for the Review Service application context.
 *
 * <p>Uses Testcontainers to spin up a disposable PostgreSQL 15 instance
 * so that the full Spring context (including JPA auto-configuration) can
 * be loaded without requiring an external database.
 *
 * <p>The single {@link #contextLoads()} test verifies that all beans are
 * wired correctly and the application starts without errors.
 *
 * @see org.springframework.boot.test.context.SpringBootTest
 * @see org.testcontainers.junit.jupiter.Testcontainers
 */
@SpringBootTest
@Testcontainers
class ReviewServiceApplicationTests {

    /**
     * Disposable PostgreSQL 15 container used as the datasource for this test.
     * {@code @ServiceConnection} auto-configures {@code spring.datasource.*}
     * properties so no manual URL/username/password wiring is needed.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    /**
     * Verifies that the Spring application context loads successfully.
     * A failure here typically indicates misconfigured beans, missing
     * properties, or entity-mapping errors.
     */
    @Test
    void contextLoads() {
    }

}
