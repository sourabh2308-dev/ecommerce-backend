package com.sourabh.product_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the Product Service microservice.
 *
 * <p>Bootstraps the application context, enables JPA auditing for automatic
 * {@code createdAt}/{@code updatedAt} population, and activates Spring's
 * task scheduler for cron-driven jobs such as flash-deal expiration.</p>
 *
 * <p>Infrastructure dependencies (PostgreSQL, Redis, Kafka, Elasticsearch,
 * Eureka) are configured via {@code application.properties} and the
 * central Config Server.</p>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class ProductServiceApplication {

	/**
	 * JVM entry point. Launches the embedded Tomcat server and initialises
	 * all Spring-managed beans.
	 *
	 * @param args command-line arguments forwarded to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

}
