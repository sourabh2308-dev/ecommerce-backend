package com.sourabh.ecommerce_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Root Spring Boot application for the E-Commerce Backend parent project.
 *
 * <p>This class serves as the entry point for the top-level Spring Boot
 * module.  In this multi-module microservices architecture each domain
 * service (auth, user, product, order, review, payment) has its own
 * {@code @SpringBootApplication} class; this parent module exists primarily
 * for shared dependency management and can be used for integration testing
 * across modules.
 *
 * <p>The {@link SpringBootApplication @SpringBootApplication} annotation
 * enables auto-configuration, component scanning within the
 * {@code com.sourabh.ecommerce_backend} package, and Spring Boot's
 * configuration property support.
 *
 * @author Sourabh
 */
@SpringBootApplication
public class EcommerceBackendApplication {

	/**
	 * Application entry point.
	 *
	 * <p>Bootstraps the Spring {@link org.springframework.context.ApplicationContext}
	 * by delegating to {@link SpringApplication#run(Class, String...)}.
	 *
	 * @param args command-line arguments forwarded to the Spring environment
	 */
	public static void main(String[] args) {
		SpringApplication.run(EcommerceBackendApplication.class, args);
	}

}
