package com.sourabh.auth_service.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-32-characters-xx";

    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateAccessToken: subject matches email")
    void generateAccessToken_subjectMatchesEmail() {
        String token = jwtUtil.generateAccessToken("alice@example.com", "uuid-1", "BUYER");

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("generateAccessToken: uuid claim is correct")
    void generateAccessToken_uuidClaimCorrect() {
        String token = jwtUtil.generateAccessToken("bob@example.com", "uuid-2", "SELLER");

        assertThat(jwtUtil.extractUserUuid(token)).isEqualTo("uuid-2");
    }

    @Test
    @DisplayName("generateAccessToken: role claim is correct")
    void generateAccessToken_roleClaimCorrect() {
        String token = jwtUtil.generateAccessToken("carol@example.com", "uuid-3", "ADMIN");

        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("generateAccessToken: repeated calls preserve claims")
    void generateAccessToken_repeatedCallsPreserveClaims() {
        String t1 = jwtUtil.generateAccessToken("dave@example.com", "uuid-4", "BUYER");
        String t2 = jwtUtil.generateAccessToken("dave@example.com", "uuid-4", "BUYER");

        assertThat(jwtUtil.extractEmail(t1)).isEqualTo("dave@example.com");
        assertThat(jwtUtil.extractEmail(t2)).isEqualTo("dave@example.com");
        assertThat(jwtUtil.extractRole(t1)).isEqualTo("BUYER");
        assertThat(jwtUtil.extractRole(t2)).isEqualTo("BUYER");
    }

    @Test
    @DisplayName("extractEmail: token from different secret throws JwtException")
    void extractEmail_wrongSecret_throwsJwtException() {
        JwtUtil other = new JwtUtil("completely-different-secret-key-x", EXPIRATION_MS);
        String token = other.generateAccessToken("eve@example.com", "uuid-5", "BUYER");

        assertThatThrownBy(() -> jwtUtil.extractEmail(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("extractEmail: expired token throws ExpiredJwtException")
    void extractEmail_expiredToken_throwsExpiredJwtException() {
        JwtUtil shortLived = new JwtUtil(SECRET, 1L); 
        String token = shortLived.generateAccessToken("fred@example.com", "uuid-6", "BUYER");

        try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertThatThrownBy(() -> jwtUtil.extractEmail(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("extractEmail: malformed token string throws JwtException")
    void extractEmail_malformedToken_throwsJwtException() {
        assertThatThrownBy(() -> jwtUtil.extractEmail("this.is.not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }
}
