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
 * Central Spring Security configuration for the reactive API Gateway.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Defines the set of <em>public</em> URL patterns that do not require
 *       authentication (login, registration, Swagger UI, OpenAPI docs, and
 *       the public product catalogue).</li>
 *   <li>Registers a custom {@link AuthenticationWebFilter} that wires together
 *       {@link JwtAuthenticationManager} (token validation) and
 *       {@link JwtAuthenticationConverter} (token extraction).</li>
 *   <li>Disables CSRF, form-login, HTTP-Basic, and logout because the
 *       gateway is a stateless API proxy that relies exclusively on JWT bearer
 *       tokens for authentication.</li>
 * </ul>
 *
 * <h3>Design Principle</h3>
 * <p>JWT validation happens <em>only</em> at the gateway. Downstream services
 * never see raw JWTs; instead, they receive pre-validated identity headers
 * ({@code X-User-UUID}, {@code X-User-Role}, {@code X-User-Email}) injected
 * by {@link InternalSecretFilterConfig}.</p>
 *
 * @see JwtAuthenticationManager
 * @see JwtAuthenticationConverter
 * @see InternalSecretFilterConfig
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * URL patterns that are accessible without authentication.
     *
     * <p>Includes authentication endpoints, user registration and OTP
     * verification, Swagger/OpenAPI documentation paths, per-service API-doc
     * endpoints used by the aggregated Swagger UI, and the public (read-only)
     * product catalogue.</p>
     */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/user/register",
            "/api/user/verify-otp",
            "/api/user/resend-otp",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/api/auth/v3/api-docs",
            "/api/user/v3/api-docs",
            "/api/product/v3/api-docs",
            "/api/order/v3/api-docs",
            "/api/review/v3/api-docs",
            "/api/payment/v3/api-docs",
            "/api/product"
    };

    /**
     * Builds the {@link SecurityWebFilterChain} that governs HTTP security
     * for the gateway.
     *
     * <p>A custom {@link AuthenticationWebFilter} is created with the
     * supplied {@code jwtAuthManager} and {@code jwtAuthConverter}, then
     * inserted at the {@link SecurityWebFiltersOrder#AUTHENTICATION} position
     * in the filter chain. All exchanges matching {@link #PUBLIC_PATHS} are
     * permitted without authentication; every other exchange requires a valid
     * JWT bearer token.</p>
     *
     * @param http             the server HTTP security builder
     * @param jwtAuthManager   reactive manager that validates JWT tokens
     * @param jwtAuthConverter converter that extracts JWT from the
     *                         {@code Authorization} header
     * @return the configured {@link SecurityWebFilterChain}
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationManager jwtAuthManager,
            JwtAuthenticationConverter jwtAuthConverter) {

        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(
                (ReactiveAuthenticationManager) jwtAuthManager);
        jwtFilter.setServerAuthenticationConverter(jwtAuthConverter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}
