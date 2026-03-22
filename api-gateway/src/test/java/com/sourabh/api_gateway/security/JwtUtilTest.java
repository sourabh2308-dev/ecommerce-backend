package com.sourabh.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("API Gateway JwtUtil Unit Tests")
class JwtUtilTest {

    private static final String SECRET = "api-gateway-test-secret-32chars-x";

    private JwtUtil jwtUtil;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    @Test
    @DisplayName("validateToken: valid token returns Claims")
    void validateToken_validToken_returnsClaims() {
        String token = buildToken("alice@example.com", "uuid-1", "BUYER", 3_600_000L);

        Claims claims = jwtUtil.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo("alice@example.com");
        assertThat(claims.get("uuid", String.class)).isEqualTo("uuid-1");
        assertThat(claims.get("role", String.class)).isEqualTo("BUYER");
    }

    @Test
    @DisplayName("validateToken: expired token throws ExpiredJwtException")
    void validateToken_expiredToken_throwsExpiredJwtException() {
        String token = buildToken("bob@example.com", "uuid-2", "SELLER", -1000L); 

        assertThatThrownBy(() -> jwtUtil.validateToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("validateToken: wrong secret throws JwtException")
    void validateToken_wrongSecret_throwsJwtException() {
        Key otherKey = Keys.hmacShaKeyFor("completely-different-key-32-chars".getBytes());
        String token = Jwts.builder()
                .setSubject("carol@example.com")
                .addClaims(Map.of("uuid", "uuid-3", "role", "ADMIN"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(otherKey, SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> jwtUtil.validateToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("validateToken: malformed token string throws JwtException")
    void validateToken_malformedToken_throwsJwtException() {
        assertThatThrownBy(() -> jwtUtil.validateToken("not.a.real.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("validateToken: empty string throws IllegalArgumentException")
    void validateToken_emptyString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> jwtUtil.validateToken(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String buildToken(String email, String uuid, String role, long expirationMs) {
        return Jwts.builder()
                .setSubject(email)
                .addClaims(Map.of("uuid", uuid, "role", role))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
