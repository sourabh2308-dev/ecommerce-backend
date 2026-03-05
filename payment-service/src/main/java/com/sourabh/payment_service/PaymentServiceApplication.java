package com.sourabh.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the <b>payment-service</b> microservice.
 *
 * <p>Responsibilities of this service:
 * <ul>
 *   <li>Processing buyer-initiated payments via REST and Kafka saga events.</li>
 *   <li>Integrating with external payment gateways (mock / Razorpay).</li>
 *   <li>Computing per-seller revenue splits (platform fee, delivery fee,
 *       seller payout).</li>
 *   <li>Publishing {@code payment.completed} events to Kafka so the order
 *       service can finalise order status.</li>
 *   <li>Providing buyer, seller, and admin financial dashboards.</li>
 * </ul>
 *
 * <p>{@code @EnableJpaAuditing} activates JPA auditing annotations such as
 * {@code @CreatedDate} and {@code @LastModifiedDate} on entity fields.
 */
@SpringBootApplication
@EnableJpaAuditing
public class PaymentServiceApplication {

    /**
     * JVM entry point.  Bootstraps the Spring application context, starts the
     * embedded Tomcat server, and begins consuming Kafka events.
     *
     * @param args command-line arguments (passed through to Spring Boot)
     */
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
