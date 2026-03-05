package com.sourabh.user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test that verifies the Spring application context loads successfully.
 *
 * <p>Uses an in-memory H2 database instead of PostgreSQL/Redis containers
 * to keep CI fast and avoid external dependencies.  Kafka auto-startup is
 * disabled so listener beans do not attempt to connect to a broker.</p>
 */
@SpringBootTest(
    properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    }
)
class UserServiceApplicationTests {

    /**
     * Ensures the full Spring context initialises without errors.
     */
    @Test
    void contextLoads() {
    }

}
