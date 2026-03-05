package com.sourabh.auth_service.controller;

import com.sourabh.auth_service.dto.request.ForgotPasswordRequest;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.request.ResetPasswordRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;
import com.sourabh.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing authentication endpoints under
 * {@code /api/auth}.
 *
 * <p>Provides login, token refresh, logout, and password-reset operations.
 * All requests are expected to arrive via the API Gateway, which attaches
 * the {@code X-Internal-Secret} header validated by
 * {@link com.sourabh.auth_service.config.InternalSecretFilter}.</p>
 *
 * <p>Annotation summary:</p>
 * <ul>
 *   <li>{@code @RestController} &ndash; marks this as a controller whose
 *       return values are serialised directly to JSON.</li>
 *   <li>{@code @RequestMapping("/api/auth")} &ndash; base path for all
 *       endpoints.</li>
 *   <li>{@code @RequiredArgsConstructor} &ndash; Lombok-generated
 *       constructor for {@code final} field injection.</li>
 *   <li>{@code @Slf4j} &ndash; Lombok-generated SLF4J logger.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    /** Business-logic service handling all authentication operations. */
    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT access / refresh token pair.
     *
     * @param request login credentials ({@code email}, {@code password})
     * @return 200 OK with {@link AuthResponse} containing the token pair
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Exchanges a valid refresh token for a new access / refresh token
     * pair.  The old refresh token is revoked (token-rotation pattern).
     *
     * @param refreshToken the refresh-token UUID issued during login or a
     *                     previous refresh
     * @return 200 OK with {@link AuthResponse} containing the new token pair
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    /**
     * Revokes the given refresh token, effectively terminating the session.
     * The JWT access token remains valid until its short-lived expiry.
     *
     * @param refreshToken the refresh-token UUID to revoke
     * @return 204 No Content on success
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    /**
     * Initiates a forgot-password flow by forwarding the request to
     * {@code user-service}, which sends a one-time password (OTP) via email.
     *
     * @param request contains the user's {@code email}
     * @return 200 OK with a generic confirmation message (prevents email
     *         enumeration)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    /**
     * Completes the password-reset flow by forwarding the email, OTP code,
     * and new password to {@code user-service} for verification and update.
     *
     * @param request contains {@code email}, {@code otpCode}, and
     *                {@code newPassword}
     * @return 200 OK with a success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
