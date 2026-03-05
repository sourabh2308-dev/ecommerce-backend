package com.sourabh.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test for the API Gateway application context.
 *
 * <p>Verifies that the Spring application context loads successfully with a
 * random port, ensuring that all bean definitions, auto-configurations, and
 * component scans are valid.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    /**
     * Asserts that the application context starts without errors.
     * An empty body is intentional &mdash; the test passes if no exception
     * is thrown during context initialisation.
     */
    @Test
    void contextLoads() {
    }
}
