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
 * Configuration class that registers a {@link GlobalFilter} responsible for
 * securing and enriching every request forwarded through the API Gateway.
 *
 * <h3>Processing Steps (executed in a single filter, single request mutation)</h3>
 * <ol>
 *   <li><strong>Strip spoofable headers</strong> &mdash; removes any
 *       client-supplied {@code X-User-UUID}, {@code X-User-Role}, and
 *       {@code X-User-Email} headers to prevent identity spoofing.</li>
 *   <li><strong>Inject validated identity from JWT</strong> &mdash; if a
 *       valid {@code Authorization: Bearer &lt;token&gt;} header is present,
 *       the token is parsed and the extracted claims (UUID, role, email) are
 *       injected as {@code X-User-*} headers. Downstream services therefore
 *       never need to parse JWTs themselves.</li>
 *   <li><strong>Inject internal secret</strong> &mdash; the configured
 *       {@code X-Internal-Secret} header is attached so downstream services
 *       can verify that the request originated from the gateway rather than
 *       from a direct external call.</li>
 * </ol>
 *
 * <p>This filter is ordered at {@code Integer.MIN_VALUE} to ensure it runs
 * before any route-level or security filters.</p>
 *
 * @see JwtUtil
 * @see SecurityConfig
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class InternalSecretFilterConfig {

    /** Utility used to validate JWT tokens and extract claims. */
    private final JwtUtil jwtUtil;

    /** Shared secret injected into every forwarded request so downstream
     *  services can reject traffic that bypasses the gateway. */
    @Value("${internal.secret}")
    private String internalSecret;

    /**
     * Creates a {@link GlobalFilter} bean that strips, injects, and secures
     * request headers on every exchange passing through the gateway.
     *
     * <p>The filter performs three operations in a single request mutation:
     * header stripping, JWT claim injection, and internal-secret injection
     * (see class-level documentation for details).</p>
     *
     * @return a {@link GlobalFilter} that secures forwarded request headers
     */
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
