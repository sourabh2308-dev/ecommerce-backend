package com.sourabh.api_gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

/**
 * Gateway-level security.
 *
 * JWT is validated HERE only — downstream services never see raw JWTs.
 * Public paths are whitelisted; everything else requires a valid Bearer token.
 */
// Spring Configuration - Defines beans and infrastructure setup
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
/**
 * SPRING SECURITY CONFIGURATION - Authentication & Authorization Setup
 * 
 * PURPOSE:
 * Configures Spring Security framework for this microservice.
 * Defines which endpoints require authentication, how tokens are validated,
 * and what roles can access specific resources.
 * 
 * KEY CONCEPTS:
 * 
 * 1. AUTHENTICATION (Who are you?)
 *    - JWT tokens validated by JwtAuthenticationFilter
 *    - User claims extracted and stored in SecurityContext
 * 
 * 2. AUTHORIZATION (What can you do?)
 *    - @PreAuthorize("hasRole('BUYER')") on controller methods
 *    - Checks if authenticated user has required role
 * 
 * CONFIGURATION COMPONENTS:
 * 
 * @Bean SecurityFilterChain:
 *   - Defines URL patterns and access rules
 *   - Example: .requestMatchers("/api/order/**").authenticated()
 *   - Registers custom filters (JWT validation, etc.)
 * 
 * @Bean PasswordEncoder:
 *   - BCrypt for hashing passwords (user-service only)
 *   - Not used in services that don't store passwords
 * 
 * CORS Configuration:
 *   - Allows cross-origin requests from frontend
 *   - Configures allowed origins, methods, headers
 * 
 * STATELESS SESSION:
 *   - sessionCreationPolicy(STATELESS)
 *   - No server-side sessions (JWT is self-contained)
 * 
 * ENDPOINT ACCESS RULES:
 * Common patterns across services:
 * 
 * PUBLIC (No authentication):
 *   - POST /api/user/register
 *   - POST /api/auth/login
 *   - GET /api/product (listing products)
 * 
 * AUTHENTICATED (Any logged-in user):
 *   - GET /api/user/profile
 *   - POST /api/order (role checked in controller)
 * 
 * ROLE-BASED (Specific roles):
 *   - POST /api/product → @PreAuthorize("hasRole('SELLER')")
 *   - GET /api/order/all → @PreAuthorize("hasRole('ADMIN')")
 * 
 * INTERNAL (Service-to-service):
 *   - POST /api/product/internal/** → Validated by InternalSecretFilter
 *   - No JWT required, uses shared secret header
 * 
 * FILTER ORDER:
 * 1. CorsFilter (handle preflight OPTIONS)
 * 2. JwtAuthenticationFilter (extract user from token)
 * 3. Spring Security filters (authorization checks)
 * 4. Controller method execution
 */
/**
 * SPRING SECURITY CONFIGURATION - Authentication & Authorization Setup
 * 
 * PURPOSE:
 * Configures Spring Security framework for this microservice.
 * Defines which endpoints require authentication, how tokens are validated,
 * and what roles can access specific resources.
 * 
 * KEY CONCEPTS:
 * 
 * 1. AUTHENTICATION (Who are you?)
 *    - JWT tokens validated by JwtAuthenticationFilter
 *    - User claims extracted and stored in SecurityContext
 * 
 * 2. AUTHORIZATION (What can you do?)
 *    - @PreAuthorize("hasRole('BUYER')") on controller methods
 *    - Checks if authenticated user has required role
 * 
 * CONFIGURATION COMPONENTS:
 * 
 * @Bean SecurityFilterChain:
 *   - Defines URL patterns and access rules
 *   - Example: .requestMatchers("/api/order/**").authenticated()
 *   - Registers custom filters (JWT validation, etc.)
 * 
 * @Bean PasswordEncoder:
 *   - BCrypt for hashing passwords (user-service only)
 *   - Not used in services that don't store passwords
 * 
 * CORS Configuration:
 *   - Allows cross-origin requests from frontend
 *   - Configures allowed origins, methods, headers
 * 
 * STATELESS SESSION:
 *   - sessionCreationPolicy(STATELESS)
 *   - No server-side sessions (JWT is self-contained)
 * 
 * ENDPOINT ACCESS RULES:
 * Common patterns across services:
 * 
 * PUBLIC (No authentication):
 *   - POST /api/user/register
 *   - POST /api/auth/login
 *   - GET /api/product (listing products)
 * 
 * AUTHENTICATED (Any logged-in user):
 *   - GET /api/user/profile
 *   - POST /api/order (role checked in controller)
 * 
 * ROLE-BASED (Specific roles):
 *   - POST /api/product → @PreAuthorize("hasRole('SELLER')")
 *   - GET /api/order/all → @PreAuthorize("hasRole('ADMIN')")
 * 
 * INTERNAL (Service-to-service):
 *   - POST /api/product/internal/** → Validated by InternalSecretFilter
 *   - No JWT required, uses shared secret header
 * 
 * FILTER ORDER:
 * 1. CorsFilter (handle preflight OPTIONS)
 * 2. JwtAuthenticationFilter (extract user from token)
 * 3. Spring Security filters (authorization checks)
 * 4. Controller method execution
 */
public class SecurityConfig {

    // Paths that do NOT require a JWT (login, register, Swagger, etc.)
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/user/register",
            "/api/user/verify-otp",
            "/api/user/resend-otp",
            // Swagger UI (served by gateway itself)
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            // Per-service OpenAPI docs fetched by the aggregated Swagger UI
            "/api/auth/v3/api-docs",
            "/api/user/v3/api-docs",
            "/api/product/v3/api-docs",
            "/api/order/v3/api-docs",
            "/api/review/v3/api-docs",
            "/api/payment/v3/api-docs",
            // Public product catalog (read-only)
            "/api/product"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationManager jwtAuthManager,
            JwtAuthenticationConverter jwtAuthConverter) {

        // Build a dedicated filter that plugs our JWT manager + converter
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(
                (ReactiveAuthenticationManager) jwtAuthManager);
        jwtFilter.setServerAuthenticationConverter(jwtAuthConverter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Register the JWT filter in the authentication slot
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}