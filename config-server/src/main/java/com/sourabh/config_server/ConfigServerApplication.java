package com.sourabh.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server — Centralized Configuration Hub.
 *
 * <p>Serves externalised configuration (key-value properties) to every
 * microservice in the platform. Each service contacts this server on
 * startup to fetch its environment-specific settings (database URLs,
 * Kafka brokers, JWT secrets, etc.) instead of bundling them locally.</p>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Config Server starts and loads properties from {@code classpath:/config/}
 *       (native profile) or a remote Git repository.</li>
 *   <li>Each microservice has {@code spring.config.import=configserver:http://config-server:8888}
 *       so Spring Boot fetches its config before completing startup.</li>
 *   <li>Properties are resolved by application name
 *       (e.g. {@code auth-service.properties} for the auth-service).</li>
 *   <li>Shared properties live in {@code application.properties} and are
 *       inherited by all services.</li>
 * </ol>
 *
 * <h3>Annotations</h3>
 * <ul>
 *   <li>{@code @SpringBootApplication} — enables auto-config and component scanning</li>
 *   <li>{@code @EnableConfigServer} — activates the Spring Cloud Config REST endpoints</li>
 * </ul>
 *
 * @see org.springframework.cloud.config.server.EnableConfigServer
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    /**
     * JVM entry point — bootstraps the Spring context and starts the
     * embedded Tomcat on port 8888.
     *
     * @param args command-line arguments (none required)
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
