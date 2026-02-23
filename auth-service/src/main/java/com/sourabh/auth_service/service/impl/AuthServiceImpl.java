package com.sourabh.auth_service.service.impl;

import com.sourabh.auth_service.dto.UserDto;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;
import com.sourabh.auth_service.entity.RefreshToken;
import com.sourabh.auth_service.exception.AuthException;
import com.sourabh.auth_service.exception.UserAccountException;
import com.sourabh.auth_service.exception.UserNotFoundException;
import com.sourabh.auth_service.repository.RefreshTokenRepository;
import com.sourabh.auth_service.service.AuthService;
import com.sourabh.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${internal.secret}")
    private String internalSecret;

    private static final String USER_BY_EMAIL_URL = "http://user-service:8080/api/user/internal/email/";
    private static final String USER_BY_UUID_URL  = "http://user-service:8080/api/user/internal/uuid/";

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        UserDto user = fetchUser(USER_BY_EMAIL_URL + request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        validateActiveAccount(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }

        String accessToken       = jwtUtil.generateAccessToken(user.getEmail(), user.getUuid(), user.getRole());
        String refreshTokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(buildRefreshToken(refreshTokenValue, user.getUuid()));

        log.info("Login successful for user: {}", user.getEmail());
        return buildAuthResponse(accessToken, refreshTokenValue);
    }

    // ─────────────────────────────────────────────
    // REFRESH TOKEN (rotation: revoke old, issue new)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new AuthException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Refresh token has expired");
        }

        UserDto user = fetchUser(USER_BY_UUID_URL + refreshToken.getUserUuid())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UserAccountException("User account is no longer active");
        }

        // Rotate: revoke old, issue new pair
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken       = jwtUtil.generateAccessToken(user.getEmail(), user.getUuid(), user.getRole());
        String newRefreshTokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(buildRefreshToken(newRefreshTokenValue, user.getUuid()));

        return buildAuthResponse(newAccessToken, newRefreshTokenValue);
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Generic internal user fetch — attaches X-Internal-Secret header.
     * Returns empty Optional on 404; logs and returns empty on other client errors.
     */
    private Optional<UserDto> fetchUser(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            UserDto body = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), UserDto.class)
                    .getBody();
            return Optional.ofNullable(body);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            log.warn("HTTP error fetching user from {}: {} {}", url, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        }
    }

    private void validateActiveAccount(UserDto user) {
        if (!user.isEmailVerified()) {
            throw new UserAccountException("Email not verified");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UserAccountException("User account is not active");
        }
    }

    private RefreshToken buildRefreshToken(String token, String userUuid) {
        return RefreshToken.builder()
                .token(token)
                .userUuid(userUuid)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    // ─────────────────────────────────────────────
    // LOGOUT — revoke the supplied refresh token
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("User logged out: userUuid={}", refreshToken.getUserUuid());
        }
    }
}
