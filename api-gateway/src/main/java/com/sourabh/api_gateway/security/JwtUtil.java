package com.sourabh.api_gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
/**
 * UTILITY CLASS - Helper Functions
 * 
 * Provides reusable helper methods for:
 *   - Token generation and validation (JWT utils)
 *   - Date/time conversions
 *   - Data transformations
 *   - Common business calculations
 */
public class JwtUtil {

    private final Key signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims validateToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
