package com.sourabh.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test verifying that the auth-service Spring application context
 * loads successfully with an in-memory H2 database substituted for
 * PostgreSQL.
 */
@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
class AuthServiceApplicationTests {

    /**
     * Verifies that the application context starts without errors.
     */
    @Test
    void contextLoads() {
    }
}
