package com.sourabh.auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for the reset-password endpoint.
 *
 * <p>Contains the user's email, the one-time password (OTP) received via
 * email, and the desired new password.  All fields are mandatory; the new
 * password must be at least six characters long.</p>
 */
@Getter
@Setter
public class ResetPasswordRequest {

    /** Email address of the account whose password is being reset. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** One-time password sent to the user's email by {@code user-service}. */
    @NotBlank(message = "OTP code is required")
    private String otpCode;

    /** New password to set (minimum 6 characters). */
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}
