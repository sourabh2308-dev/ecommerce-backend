package com.sourabh.auth_service.service.impl;

import com.sourabh.auth_service.dto.UserDto;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;
import com.sourabh.auth_service.entity.RefreshToken;
import com.sourabh.auth_service.repository.RefreshTokenRepository;
import com.sourabh.auth_service.service.AuthService;
import com.sourabh.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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

    private static final String USER_SERVICE_URL =
            "http://localhost:8082/api/users/internal/email/";

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());

        // 1️⃣ Fetch user from user-service
        UserDto user = restTemplate.getForObject(
                USER_SERVICE_URL + request.getEmail(),
                UserDto.class
        );

        if (user == null) {
            throw new RuntimeException("Invalid credentials");
        }

        // 2️⃣ Check user status
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("User account is not active");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified");
        }

        // 3️⃣ Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // 4️⃣ Generate access token
        String accessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getUuid(),
                user.getRole()
        );

        // 5️⃣ Generate refresh token
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userUuid(user.getUuid())
                .expiryDate(LocalDateTime.now()
                        .plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Login successful for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked() ||
                refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        // Ideally fetch user again from user-service
        UserDto user = restTemplate.getForObject(
                USER_SERVICE_URL + refreshToken.getUserUuid(),
                UserDto.class
        );

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getUuid(),
                user.getRole()
        );

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .build();
    }
}
