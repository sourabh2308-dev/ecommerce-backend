package com.sourabh.auth_service.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * Utility component for creating, parsing, and validating JSON Web Tokens.
 *
 * <p>Tokens are signed with HS256 (HMAC-SHA256) and carry the following
 * claims:</p>
 * <ul>
 *   <li>{@code sub} &ndash; user email (standard subject claim)</li>
 *   <li>{@code uuid} &ndash; user unique identifier</li>
 *   <li>{@code role} &ndash; user role ({@code BUYER}, {@code SELLER},
 *       {@code ADMIN})</li>
 *   <li>{@code iat} &ndash; issued-at timestamp</li>
 *   <li>{@code exp} &ndash; expiration timestamp</li>
 * </ul>
 *
 * <p>The signing secret and access-token lifetime are injected from
 * application properties ({@code jwt.secret} and
 * {@code jwt.access-token-expiration}).</p>
 */
@Component
public class JwtUtil {

    /** HMAC-SHA256 signing key derived from the configured secret. */
    private final Key signingKey;

    /** Access-token lifetime in milliseconds. */
    private final long accessTokenExpiration;

    /**
     * Constructs the utility, converting the secret string into a
     * cryptographic {@link Key} suitable for HS256 signing.
     *
     * @param secret               JWT signing secret (min 32 characters)
     * @param accessTokenExpiration token lifetime in milliseconds
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /**
     * Generates a signed JWT access token containing user identity claims.
     *
     * @param email    user's email address (JWT subject)
     * @param userUuid user's UUID (custom claim)
     * @param role     user's role (custom claim)
     * @return a compact, Base64-encoded JWT string
     */
    public String generateAccessToken(String email, String userUuid, String role) {
        return Jwts.builder()
                .setSubject(email)
                .addClaims(Map.of(
                        "uuid", userUuid,
                        "role", role
                ))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject claim) from the given token.
     *
     * @param token JWT string
     * @return the user's email address
     * @throws JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the user UUID (custom claim) from the given token.
     *
     * @param token JWT string
     * @return the user's UUID
     * @throws JwtException if the token is invalid or expired
     */
    public String extractUserUuid(String token) {
        return extractAllClaims(token).get("uuid", String.class);
    }

    /**
     * Extracts the user role (custom claim) from the given token.
     *
     * @param token JWT string
     * @return the user's role ({@code BUYER}, {@code SELLER}, or
     *         {@code ADMIN})
     * @throws JwtException if the token is invalid or expired
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Validates that the token signature is correct and the token has not
     * expired.
     *
     * @param token JWT string to validate
     * @return {@code true} if valid; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses the token, verifies its HS256 signature, and returns all
     * embedded claims.
     *
     * @param token JWT string
     * @return the {@link Claims} payload
     * @throws JwtException if signature verification or parsing fails
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
