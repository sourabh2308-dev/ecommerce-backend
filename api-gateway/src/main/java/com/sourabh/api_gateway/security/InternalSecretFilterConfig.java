package com.sourabh.api_gateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Gateway-level security filters applied to every forwarded request.
 *
 * Order of operations (single filter, single mutate call):
 *  1. Strip any client-supplied X-User-* headers (prevent header spoofing).
 *  2. If a valid Bearer token is present, extract JWT claims and inject
 *     X-User-UUID, X-User-Role, X-User-Email so downstream services know
 *     who the caller is without ever touching a JWT themselves.
 *  3. Inject X-Internal-Secret so downstream services can reject requests
 *     that bypass the gateway entirely.
 */
@Slf4j
// Spring Configuration - Defines beans and infrastructure setup
@Configuration
@RequiredArgsConstructor
public class InternalSecretFilterConfig {

    private final JwtUtil jwtUtil;

    @Value("${internal.secret}")
    private String internalSecret;

    @Bean
    @Order(Integer.MIN_VALUE) // Run before any route-level filter
    public GlobalFilter securityHeadersFilter() {
        return (exchange, chain) -> {

            ServerHttpRequest.Builder mutated = exchange.getRequest().mutate();

            // ── Step 1: Strip spoofable identity headers from the incoming client request ──
            mutated.headers(h -> {
                h.remove("X-User-UUID");
                h.remove("X-User-Role");
                h.remove("X-User-Email");
            });

            // ── Step 2: Inject validated identity from JWT (if present) ──
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Claims claims = jwtUtil.validateToken(token);
                    String uuid = claims.get("uuid", String.class);
                    String role = claims.get("role", String.class);
                    String email = claims.getSubject();

                    if (uuid != null)  mutated.header("X-User-UUID", uuid);
                    if (role != null)  mutated.header("X-User-Role", role);
                    if (email != null) mutated.header("X-User-Email", email);

                } catch (Exception e) {
                    // JWT is invalid — SecurityConfig will have already rejected
                    // authenticated routes. For public routes we just skip headers.
                    log.debug("Could not extract JWT claims for header injection: {}", e.getMessage());
                }
            }

            // ── Step 3: Inject internal secret so downstream services reject direct access ──
            mutated.header("X-Internal-Secret", internalSecret);

            return chain.filter(
                    exchange.mutate().request(mutated.build()).build()
            );
        };
    }
}