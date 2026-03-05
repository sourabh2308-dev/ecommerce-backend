package com.sourabh.api_gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * Utility component for JWT (JSON Web Token) validation within the API
 * Gateway.
 *
 * <p>This class is intentionally <em>read-only</em>: it validates and parses
 * tokens but never issues them. Token creation is the responsibility of the
 * {@code auth-service}. The gateway only needs to verify that incoming tokens
 * are authentic and unexpired before forwarding requests to downstream
 * microservices.</p>
 *
 * <h3>Key Management</h3>
 * <p>The HMAC-SHA signing key is derived from the {@code jwt.secret}
 * application property (injected via {@code @Value}). In production this
 * value is supplied through an environment variable so the same secret is
 * shared between the auth-service (which signs tokens) and the gateway
 * (which verifies them).</p>
 *
 * @see JwtAuthenticationManager
 * @see JwtAuthenticationConverter
 */
@Component
public class JwtUtil {

    /** HMAC-SHA key used to verify JWT signatures. */
    private final Key signingKey;

    /**
     * Constructs the utility and initialises the HMAC-SHA signing key from
     * the provided secret string.
     *
     * @param secret the shared JWT secret, injected from
     *               {@code jwt.secret} application property
     */
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Validates a JWT token's signature and expiration, then returns the
     * embedded claims.
     *
     * <p>Throws a subclass of {@link JwtException} (e.g.
     * {@link ExpiredJwtException}, {@link MalformedJwtException}) if the
     * token is invalid.</p>
     *
     * @param token the compact JWT string (without the {@code "Bearer "}
     *              prefix)
     * @return the {@link Claims} body extracted from the token
     * @throws JwtException if signature verification or parsing fails
     */
    public Claims validateToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
