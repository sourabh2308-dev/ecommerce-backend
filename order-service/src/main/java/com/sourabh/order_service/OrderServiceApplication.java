package com.sourabh.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the <strong>Order Service</strong> microservice.
 *
 * <p>This Spring Boot application manages the complete order lifecycle including
 * creation, status transitions, payments, returns, invoicing, coupon management,
 * shipment tracking, and multi-seller order splitting.</p>
 *
 * <h3>Enabled Capabilities</h3>
 * <ul>
 *   <li>{@code @SpringBootApplication} – component scanning, auto-configuration,
 *       and property binding.</li>
 *   <li>{@code @EnableJpaAuditing} – automatic population of {@code createdAt} /
 *       {@code updatedAt} fields on JPA entities.</li>
 *   <li>{@code @EnableFeignClients} – declarative HTTP clients for inter-service
 *       communication (product-service, user-service).</li>
 *   <li>{@code @EnableScheduling} – cron-based tasks such as auto-cancellation of
 *       unpaid orders and coupon expiration.</li>
 * </ul>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableScheduling
public class OrderServiceApplication {

    /**
     * Bootstraps the Order Service by delegating to {@link SpringApplication#run}.
     *
     * <p>Spring Boot initialises the embedded Tomcat server, connects to PostgreSQL,
     * Redis, and Kafka, registers with Eureka, and starts all scheduled tasks.</p>
     *
     * @param args command-line arguments forwarded to the Spring environment
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
