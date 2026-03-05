package com.sourabh.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Discovery Server.
 *
 * <p>Acts as the central registry for all microservices in the platform.
 * Every service (auth, user, product, order, payment, review) registers
 * itself here on startup and sends periodic heartbeats. Other services
 * (and the API Gateway) query this registry to locate service instances
 * by logical name instead of hard-coded URLs.</p>
 *
 * <h3>Key responsibilities</h3>
 * <ul>
 *   <li>Maintains an in-memory registry of service instances and their network locations</li>
 *   <li>Evicts instances that stop sending heartbeats (default 90 s)</li>
 *   <li>Exposes a dashboard at <code>http://localhost:8761</code> for visual inspection</li>
 *   <li>Provides REST API consumed by Spring Cloud load-balancers and Feign clients</li>
 * </ul>
 *
 * <h3>Annotations</h3>
 * <ul>
 *   <li>{@code @SpringBootApplication} — enables auto-configuration and component scanning</li>
 *   <li>{@code @EnableEurekaServer} — activates the embedded Netflix Eureka server</li>
 * </ul>
 *
 * @see org.springframework.cloud.netflix.eureka.server.EnableEurekaServer
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    /**
     * JVM entry point — bootstraps the Spring context and starts the
     * embedded Tomcat server on port 8761.
     *
     * @param args command-line arguments (none required)
     */
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
