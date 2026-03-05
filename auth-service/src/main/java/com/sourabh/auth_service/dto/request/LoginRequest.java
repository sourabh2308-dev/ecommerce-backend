package com.sourabh.auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for the login endpoint.
 *
 * <p>Captures the user's email and password submitted during authentication.
 * Both fields are validated with {@code @NotBlank}; the email is additionally
 * validated with {@code @Email}.  Validation failures result in a 400 Bad
 * Request handled by
 * {@link com.sourabh.auth_service.exception.GlobalExceptionHandler}.</p>
 */
@Getter
@Setter
public class LoginRequest {

    /** User's email address (authentication principal). */
    @Email
    @NotBlank
    private String email;

    /** User's plain-text password (compared against BCrypt hash in user-service). */
    @NotBlank
    private String password;
}
