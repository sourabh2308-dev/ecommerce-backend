package com.sourabh.auth_service.service;

import com.sourabh.auth_service.dto.request.ForgotPasswordRequest;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.request.ResetPasswordRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;

/**
 * Service interface defining the authentication operations exposed by the
 * auth-service.
 *
 * <p>Implementations coordinate credential validation (via
 * {@code user-service}), JWT token generation, refresh-token rotation,
 * session termination, and password-reset flows.</p>
 *
 * @see com.sourabh.auth_service.service.impl.AuthServiceImpl
 */
public interface AuthService {

    /**
     * Authenticates a user by email/password and returns a token pair.
     *
     * @param request login credentials
     * @return access and refresh tokens
     */
    AuthResponse login(LoginRequest request);

    /**
     * Rotates the given refresh token, returning a new token pair.
     *
     * @param refreshToken the current refresh-token UUID
     * @return new access and refresh tokens
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Revokes the given refresh token (logout).
     *
     * @param refreshToken the refresh-token UUID to revoke
     */
    void logout(String refreshToken);

    /**
     * Initiates a forgot-password flow by delegating to
     * {@code user-service}.
     *
     * @param request contains the user's email
     * @return a generic confirmation message
     */
    String forgotPassword(ForgotPasswordRequest request);

    /**
     * Completes a password reset using the OTP sent to the user's email.
     *
     * @param request contains email, OTP code, and new password
     * @return success message
     */
    String resetPassword(ResetPasswordRequest request);
}
