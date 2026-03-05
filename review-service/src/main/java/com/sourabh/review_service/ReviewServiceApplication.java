package com.sourabh.review_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Spring Boot entry point for the <strong>review-service</strong> microservice.
 *
 * <p>Bootstraps the Spring application context, component-scanning, and
 * auto-configuration for the review domain. Enables:
 * <ul>
 *   <li>{@link EnableJpaAuditing} — automatic {@code createdAt} /
 *       {@code updatedAt} timestamps on JPA entities.</li>
 *   <li>{@link EnableFeignClients} — classpath scanning for Feign client
 *       interfaces (e.g.&nbsp;{@link com.sourabh.review_service.feign.OrderServiceClient}).</li>
 * </ul>
 *
 * <p>The application listens on the port defined by {@code server.port}
 * (default {@code 8080}) and connects to PostgreSQL, Redis, Kafka, Eureka,
 * and Config Server as configured in {@code application.properties}.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
public class ReviewServiceApplication {

    /**
     * JVM entry point. Delegates to {@link SpringApplication#run} to
     * initialise and start the embedded servlet container.
     *
     * @param args command-line arguments (forwarded to Spring Boot)
     */
    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }
}
