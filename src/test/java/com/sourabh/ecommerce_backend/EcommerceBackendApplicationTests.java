package com.sourabh.ecommerce_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test for the root {@link EcommerceBackendApplication}.
 *
 * <p>Verifies that the Spring {@link org.springframework.context.ApplicationContext}
 * loads successfully with the default configuration.  A failure here indicates
 * a misconfiguration in component scanning, bean wiring, or property resolution
 * at the parent-module level.
 *
 * @author Sourabh
 */
@SpringBootTest
class EcommerceBackendApplicationTests {

	/**
	 * Ensures the application context starts without errors.
	 *
	 * <p>This intentionally empty test method relies on the
	 * {@link SpringBootTest @SpringBootTest} annotation to bootstrap the
	 * full context; if any bean fails to initialize the test will fail.
	 */
	@Test
	void contextLoads() {
	}

}
