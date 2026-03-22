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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InternalSecretFilterConfig {

    private final JwtUtil jwtUtil;

    @Value("${internal.secret}")
    private String internalSecret;

    @Bean
    @Order(Integer.MIN_VALUE)
    public GlobalFilter securityHeadersFilter() {
        return (exchange, chain) -> {

            ServerHttpRequest.Builder mutated = exchange.getRequest().mutate();

            mutated.headers(h -> {
                h.remove("X-User-UUID");
                h.remove("X-User-Role");
                h.remove("X-User-Email");
            });

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
                    log.debug("Could not extract JWT claims for header injection: {}", e.getMessage());
                }
            }

            mutated.header("X-Internal-Secret", internalSecret);

            return chain.filter(
                    exchange.mutate().request(mutated.build()).build()
            );
        };
    }
}
