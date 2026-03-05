package com.sourabh.auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for the forgot-password endpoint.
 *
 * <p>Contains the email address of the account whose password should be
 * reset.  Validated with {@code @NotBlank} and {@code @Email} before
 * forwarding to {@code user-service}.</p>
 */
@Getter
@Setter
public class ForgotPasswordRequest {

    /** Email address of the account requesting a password reset. */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
