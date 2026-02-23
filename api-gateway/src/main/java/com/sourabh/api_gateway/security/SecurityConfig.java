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
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
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