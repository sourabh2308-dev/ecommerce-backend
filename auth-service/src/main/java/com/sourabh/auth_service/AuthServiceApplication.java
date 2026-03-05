package com.sourabh.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Entry point for the Authentication Service microservice.
 *
 * <p>This service handles user login, JWT access/refresh token generation,
 * token refresh rotation, logout (token revocation), and password-reset
 * flows.  It does not store user data directly; user lookups are delegated
 * to {@code user-service} via internal REST calls.</p>
 *
 * <ul>
 *   <li>{@code @SpringBootApplication} combines {@code @Configuration},
 *       {@code @EnableAutoConfiguration}, and {@code @ComponentScan}.</li>
 *   <li>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} and
 *       {@code @Secured} on controller/service methods.</li>
 *   <li>{@code @EnableJpaAuditing} auto-populates {@code @CreatedDate}
 *       and {@code @LastModifiedDate} fields on JPA entities.</li>
 * </ul>
 */
@EnableMethodSecurity
@SpringBootApplication
@EnableJpaAuditing
public class AuthServiceApplication {

    /**
     * Bootstraps the Spring Boot application, starting the embedded Tomcat
     * server, initialising the application context, and registering all
     * managed beans.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
