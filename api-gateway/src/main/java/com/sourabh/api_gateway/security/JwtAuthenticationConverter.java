package com.sourabh.api_gateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive authentication converter that extracts a JWT bearer token from
 * the incoming HTTP request and wraps it in a Spring Security
 * {@link Authentication} object.
 *
 * <h3>How It Fits in the Security Chain</h3>
 * <p>This converter is registered on the {@link org.springframework.security.web.server.authentication.AuthenticationWebFilter}
 * created in {@link SecurityConfig}. When a request arrives:</p>
 * <ol>
 *   <li>This converter inspects the {@code Authorization} header.</li>
 *   <li>If a {@code Bearer} token is found, it is placed into a
 *       {@link UsernamePasswordAuthenticationToken} (the raw token string
 *       serves as both principal and credentials at this stage).</li>
 *   <li>The token is then handed to {@link JwtAuthenticationManager} for
 *       actual validation and claim extraction.</li>
 * </ol>
 *
 * <p>If no {@code Authorization} header is present, or it does not start with
 * {@code "Bearer "}, the converter returns {@link Mono#empty()} and the
 * request continues unauthenticated (which is fine for public endpoints).</p>
 *
 * @see JwtAuthenticationManager
 * @see SecurityConfig
 */
@Component
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    /**
     * Attempts to extract a JWT bearer token from the exchange's
     * {@code Authorization} header.
     *
     * @param exchange the current server exchange
     * @return a {@link Mono} emitting an {@link Authentication} containing the
     *         raw JWT string, or {@link Mono#empty()} if no bearer token is
     *         present
     */
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.empty();
        }

        String token = authHeader.substring(7);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(token, token);

        return Mono.just(authentication);
    }
}
