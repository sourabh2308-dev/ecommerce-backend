package com.sourabh.auth_service.service.impl;

import com.sourabh.auth_service.dto.UserDto;
import com.sourabh.auth_service.dto.request.ForgotPasswordRequest;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.request.ResetPasswordRequest;
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
import org.springframework.web.util.UriComponentsBuilder;
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

    @Value("${service.user.base-url:http://user-service:8080}")
    private String userServiceBaseUrl;

    private String userByEmailUrl() {
        return userServiceBaseUrl + "/api/user/internal/email/";
    }

    private String userByUuidUrl() {
        return userServiceBaseUrl + "/api/user/internal/uuid/";
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        log.info("Login attempt for email: {}", normalizedEmail);

        UserDto user = fetchUser(userByEmailUrl() + normalizedEmail)
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

        UserDto user = fetchUser(userByUuidUrl() + refreshToken.getUserUuid())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String refreshStatus = user.getStatus();
        if (!("ACTIVE".equalsIgnoreCase(refreshStatus)
                || "PENDING_DETAILS".equalsIgnoreCase(refreshStatus)
                || "PENDING_APPROVAL".equalsIgnoreCase(refreshStatus))) {
            throw new UserAccountException("User account is no longer active");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken       = jwtUtil.generateAccessToken(user.getEmail(), user.getUuid(), user.getRole());
        String newRefreshTokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(buildRefreshToken(newRefreshTokenValue, user.getUuid()));

        return buildAuthResponse(newAccessToken, newRefreshTokenValue);
    }

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

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        log.info("Forgot password request for email: {}", email);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String url = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl + "/api/user/internal/forgot-password")
                    .queryParam("email", email)
                    .toUriString();
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            return "If the email exists, a password reset OTP has been sent.";
        } catch (HttpClientErrorException e) {
            log.warn("Forgot password error for {}: {}", email, e.getMessage());
            return "If the email exists, a password reset OTP has been sent.";
        }
    }

    @Override
    public String resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        log.info("Password reset attempt for email: {}", email);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String url = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl + "/api/user/internal/reset-password")
                    .queryParam("email", email)
                    .queryParam("otpCode", request.getOtpCode())
                    .queryParam("newPassword", request.getNewPassword())
                    .toUriString();
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            return "Password reset successfully";
        } catch (HttpClientErrorException e) {
            log.warn("Password reset error for {}: {}", email, e.getMessage());
            throw new AuthException("Password reset failed: " + e.getResponseBodyAsString());
        }
    }

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

        String status = user.getStatus();

        if ("ACTIVE".equalsIgnoreCase(status)
                || "PENDING_DETAILS".equalsIgnoreCase(status)
                || "PENDING_APPROVAL".equalsIgnoreCase(status)) {
            return;
        }

        throw new UserAccountException("User account is not active");
    }

    private RefreshToken buildRefreshToken(String token, String userUuid) {
        return RefreshToken.builder()
                .token(token)
                .userUuid(userUuid)
                .expiryDate(LocalDateTime.now()
                        .plusSeconds(refreshTokenExpiration / 1000))
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
}
