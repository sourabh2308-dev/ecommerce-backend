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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Core implementation of {@link AuthService}, providing authentication
 * business logic for the auth-service microservice.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Credential validation &ndash; fetches user data from
 *       {@code user-service} via REST and compares the submitted password
 *       against a BCrypt hash.</li>
 *   <li>JWT access-token generation &ndash; short-lived, stateless tokens
 *       containing the user's email, UUID, and role.</li>
 *   <li>Refresh-token management &ndash; creates, stores, rotates, and
 *       revokes opaque UUID tokens persisted in the {@code refresh_token}
 *       table.</li>
 *   <li>Password-reset orchestration &ndash; forwards forgot/reset password
 *       requests to {@code user-service} for OTP generation and
 *       verification.</li>
 * </ul>
 *
 * <p>Security highlights:</p>
 * <ul>
 *   <li>Passwords are never stored locally; BCrypt comparison runs
 *       in-memory against the hash retrieved from {@code user-service}.</li>
 *   <li>Refresh tokens follow a rotation pattern &ndash; each use revokes
 *       the old token and issues a new one.</li>
 *   <li>Service-to-service calls carry an {@code X-Internal-Secret}
 *       header.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    /** Synchronous HTTP client for REST calls to {@code user-service}. */
    private final RestTemplate restTemplate;

    /** Utility for generating and validating JWT access tokens. */
    private final JwtUtil jwtUtil;

    /** BCrypt encoder for comparing plain-text input against stored hashes. */
    private final PasswordEncoder passwordEncoder;

    /** JPA repository for persisting and querying {@link RefreshToken} entities. */
    private final RefreshTokenRepository refreshTokenRepository;

    /** Refresh-token lifetime in milliseconds (default 7 days). */
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /** Shared secret attached as {@code X-Internal-Secret} on calls to {@code user-service}. */
    @Value("${internal.secret}")
    private String internalSecret;

    /** Base URL of the user-service (resolved via Docker service name). */
    @Value("${service.user.base-url:http://user-service:8080}")
    private String userServiceBaseUrl;

    /**
     * Returns the user-service URL for fetching a user by email.
     *
     * @return URL ending with {@code /api/user/internal/email/}
     */
    private String userByEmailUrl() {
        return userServiceBaseUrl + "/api/user/internal/email/";
    }

    /**
     * Returns the user-service URL for fetching a user by UUID.
     *
     * @return URL ending with {@code /api/user/internal/uuid/}
     */
    private String userByUuidUrl() {
        return userServiceBaseUrl + "/api/user/internal/uuid/";
    }

    /**
     * Authenticates a user and issues a JWT access / refresh token pair.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Normalise email to lower-case and trim whitespace.</li>
     *   <li>Fetch the user record from {@code user-service} by email.</li>
     *   <li>Validate that the account is active and email is verified.</li>
     *   <li>Compare the submitted password against the BCrypt hash.</li>
     *   <li>Generate a JWT access token and a UUID refresh token.</li>
     *   <li>Persist the refresh token with its expiry date.</li>
     *   <li>Return the token pair.</li>
     * </ol>
     *
     * @param request login credentials (email and password)
     * @return {@link AuthResponse} containing the token pair
     * @throws AuthException         if credentials are invalid
     * @throws UserAccountException  if the account is inactive or unverified
     */
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

    /**
     * Rotates the given refresh token, returning a fresh token pair.
     *
     * <p>Implements the secure token-rotation pattern:</p>
     * <ol>
     *   <li>Look up the refresh token in the database.</li>
     *   <li>Verify it has not been revoked and has not expired.</li>
     *   <li>Confirm the owning user still exists and is in an allowed
     *       state.</li>
     *   <li>Revoke the old refresh token.</li>
     *   <li>Generate and persist a new token pair.</li>
     * </ol>
     *
     * @param refreshTokenValue the current refresh-token UUID
     * @return {@link AuthResponse} containing the new token pair
     * @throws AuthException          if the token is invalid, revoked, or
     *                                expired
     * @throws UserNotFoundException  if the owning user no longer exists
     * @throws UserAccountException   if the user's account is no longer
     *                                active
     */
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

    /**
     * Revokes the specified refresh token, effectively logging the user out.
     *
     * <p>The operation is idempotent: calling it on an already-revoked token
     * has no effect.  Note that the associated JWT access token remains
     * valid until its short-lived expiry (stateless design trade-off).</p>
     *
     * @param refreshTokenValue the refresh-token UUID to revoke
     * @throws AuthException if the token does not exist
     */
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

    /**
     * Initiates a forgot-password flow by forwarding the request to
     * {@code user-service}, which generates and emails an OTP.
     *
     * <p>Returns a generic message regardless of whether the email exists,
     * preventing email-enumeration attacks.</p>
     *
     * @param request contains the user's email address
     * @return a fixed confirmation message
     */
    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        log.info("Forgot password request for email: {}", email);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String url = userServiceBaseUrl + "/api/user/internal/forgot-password?email=" + email;
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            return "If the email exists, a password reset OTP has been sent.";
        } catch (HttpClientErrorException e) {
            log.warn("Forgot password error for {}: {}", email, e.getMessage());
            return "If the email exists, a password reset OTP has been sent.";
        }
    }

    /**
     * Completes the password-reset flow by forwarding the email, OTP, and
     * new password to {@code user-service} for verification and update.
     *
     * @param request contains email, OTP code, and new password
     * @return success message
     * @throws AuthException if the OTP is invalid or the reset fails
     */
    @Override
    public String resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        log.info("Password reset attempt for email: {}", email);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String url = userServiceBaseUrl + "/api/user/internal/reset-password"
                    + "?email=" + email
                    + "&otpCode=" + request.getOtpCode()
                    + "&newPassword=" + request.getNewPassword();
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            return "Password reset successfully";
        } catch (HttpClientErrorException e) {
            log.warn("Password reset error for {}: {}", email, e.getMessage());
            throw new AuthException("Password reset failed: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Makes an authenticated HTTP GET request to a {@code user-service}
     * internal endpoint, attaching the {@code X-Internal-Secret} header.
     *
     * @param url the full URL including the user identifier (email or UUID)
     * @return an {@link Optional} containing the user DTO, or empty if the
     *         user was not found (HTTP 404) or a client error occurred
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

    /**
     * Validates that the user's account is eligible for authentication.
     *
     * <p>Requirements:</p>
     * <ul>
     *   <li>Email must be verified.</li>
     *   <li>Status must be {@code ACTIVE}, {@code PENDING_DETAILS}, or
     *       {@code PENDING_APPROVAL}.  Sellers in onboarding can log in to
     *       complete their profile or check approval status.</li>
     * </ul>
     *
     * @param user the user DTO fetched from {@code user-service}
     * @throws UserAccountException if the account does not meet the rules
     */
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

    /**
     * Constructs a {@link RefreshToken} entity with a calculated expiry.
     *
     * @param token    the UUID token value
     * @param userUuid the UUID of the owning user
     * @return a new, non-revoked {@link RefreshToken} ready to persist
     */
    private RefreshToken buildRefreshToken(String token, String userUuid) {
        return RefreshToken.builder()
                .token(token)
                .userUuid(userUuid)
                .expiryDate(LocalDateTime.now()
                        .plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();
    }

    /**
     * Builds an {@link AuthResponse} wrapping the given tokens.
     *
     * @param accessToken  the JWT access token
     * @param refreshToken the UUID refresh token
     * @return a response DTO with {@code tokenType} set to {@code "Bearer"}
     */
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
