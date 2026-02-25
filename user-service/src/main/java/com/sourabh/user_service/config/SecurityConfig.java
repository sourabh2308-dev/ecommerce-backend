package com.sourabh.user_service.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * User-service security config.
 *
 * JWT validation is handled exclusively at the API Gateway.
 * The gateway injects X-User-Role / X-User-UUID headers which are read by
 * {@link HeaderRoleAuthenticationFilter} to populate the Spring Security context,
 * enabling {@code @PreAuthorize} evaluation on controller methods.
 */
// Spring Configuration - Defines beans and infrastructure setup
@Configuration
@EnableMethodSecurity
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

    private final HeaderRoleAuthenticationFilter headerRoleAuthFilter;

    @Bean
    /**
     * SECURITYFILTERCHAIN - Method Documentation
     *
     * PURPOSE:
     * This method handles the securityFilterChain operation.
     *
     * PARAMETERS:
     * @param http - HttpSecurity value
     *
     * RETURN VALUE:
     * @return SecurityFilterChain - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Bean - Applied to this method
     *
     */
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(headerRoleAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    /**
     * PASSWORDENCODER - Method Documentation
     *
     * PURPOSE:
     * This method handles the passwordEncoder operation.
     *
     * RETURN VALUE:
     * @return PasswordEncoder - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Bean - Applied to this method
     *
     */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
