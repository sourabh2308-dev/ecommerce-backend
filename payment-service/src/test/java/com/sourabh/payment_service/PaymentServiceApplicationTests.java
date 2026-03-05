package com.sourabh.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration smoke test that verifies the Spring application context
 * loads successfully for the Payment Service.
 *
 * <p>Uses <strong>Testcontainers</strong> to spin up a disposable
 * PostgreSQL instance. Kafka auto-startup is disabled to avoid requiring
 * a running broker during context-load verification.
 *
 * <p>Currently {@code @Disabled} so that the CI pipeline only runs fast
 * unit tests. Enable manually when validating full wiring.
 *
 * @see org.springframework.boot.test.context.SpringBootTest
 */
@SpringBootTest(properties = {
    "spring.kafka.listener.auto-startup=false"
})
@Testcontainers
@org.junit.jupiter.api.Disabled("skip integration context load during unit test runs")
class PaymentServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Test
    void contextLoads() {
    }

}

