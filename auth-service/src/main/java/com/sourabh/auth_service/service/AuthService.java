package com.sourabh.auth_service.service;

import com.sourabh.auth_service.dto.request.ForgotPasswordRequest;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.request.ResetPasswordRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;

/**
 * SERVICE INTERFACE - Business Logic Contract
 * 
 * Defines the public API for business operations.
 * All implementing classes must follow these contracts.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    String forgotPassword(ForgotPasswordRequest request);

    String resetPassword(ResetPasswordRequest request);
}
