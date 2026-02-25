package com.sourabh.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * AUTH SERVICE - MAIN APPLICATION CLASS
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * This is the entry point for the Authentication Service microservice.
 * The auth-service is responsible for:
 *   1. User login (validating credentials against user-service)
 *   2. JWT token generation (access tokens containing user identity and role)
 *   3. Refresh token management (secure token rotation for session renewal)
 *   4. User logout (revoking refresh tokens)
 * 
 * ARCHITECTURE ROLE:
 * -----------------
 * - This service DOES NOT store or manage user data directly
 * - It delegates user lookups to user-service via REST calls
 * - It issues stateless JWT tokens that the API Gateway validates
 * - It maintains refresh tokens in its own database for secure rotation
 * 
 * SPRING BOOT ANNOTATIONS EXPLAINED:
 * ----------------------------------
 * 
 * @SpringBootApplication:
 *   - This is a meta-annotation combining three essential annotations:
 *     1. @Configuration: Marks this class as a source of bean definitions
 *     2. @EnableAutoConfiguration: Tells Spring Boot to auto-configure beans based
 *        on classpath dependencies (e.g., if JPA is present, configure DataSource)
 *     3. @ComponentScan: Scans current package and sub-packages for @Component,
 *        @Service, @Repository, @Controller beans and registers them
 *   - Essentially bootstraps the entire Spring application context
 * 
 * @EnableMethodSecurity:
 *   - Activates method-level security annotations like @PreAuthorize, @Secured
 *   - Allows fine-grained access control on controller/service methods
 *   - Example: @PreAuthorize("hasRole('ADMIN')")
 *   - Uses Spring AOP (Aspect-Oriented Programming) to intercept method calls
 *   - Replaces the older @EnableGlobalMethodSecurity (deprecated)
 * 
 * @EnableJpaAuditing:
 *   - Enables automatic auditing of JPA entities
 *   - Automatically populates fields annotated with:
 *     - @CreatedDate: Sets timestamp when entity is first persisted
 *     - @LastModifiedDate: Updates timestamp on every entity modification
 *     - @CreatedBy: Captures who created the entity (requires AuditorAware bean)
 *     - @LastModifiedBy: Captures who last modified the entity
 *   - Useful for tracking entity lifecycle without manual timestamp management
 *   - In this service, used for RefreshToken entity auditing
 * 
 * HOW IT WORKS:
 * -------------
 * 1. SpringApplication.run() bootstraps the embedded Tomcat server
 * 2. Spring Boot scans classpath and auto-configures:
 *    - PostgreSQL DataSource (from spring.datasource.* properties)
 *    - JPA/Hibernate for entity management
 *    - Spring Security filter chain
 *    - Embedded Tomcat on port specified in application.properties
 * 3. All @Component, @Service, @Repository beans are instantiated and injected
 * 4. Server starts listening for HTTP requests on configured port
 * 
 * DEPENDENCIES:
 * -------------
 * - Spring Boot Starter Web: REST API support
 * - Spring Boot Starter Data JPA: Database access via Hibernate
 * - Spring Boot Starter Security: Security filters, password encoding
 * - PostgreSQL Driver: Database connectivity
 * - JJWT: JWT token creation and validation
 * - Spring Cloud Config Client: Fetches configuration from config-server
 * - Spring Cloud Netflix Eureka Client: Service discovery registration
 * 
 * CONFIGURATION:
 * --------------
 * - Fetches config from config-server (http://config-server:8888)
 * - Registers with eureka-server for service discovery
 * - Database: auth_db (created by init.sql in docker-compose setup)
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */
@EnableMethodSecurity      // Enables @PreAuthorize, @Secured on methods
@SpringBootApplication     // Main Spring Boot application marker
@EnableJpaAuditing        // Auto-populate @CreatedDate, @LastModifiedDate fields
public class AuthServiceApplication {
    
    /**
     * Main entry point for the Java application.
     * 
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        // SpringApplication.run() starts the Spring Boot application:
        // 1. Creates ApplicationContext (IoC container)
        // 2. Scans for @Component-annotated classes
        // 3. Registers beans and performs dependency injection
        // 4. Starts embedded Tomcat server
        // 5. Initializes database connections
        // 6. Applies Flyway migrations (if configured)
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

