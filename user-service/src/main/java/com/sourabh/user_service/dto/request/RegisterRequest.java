package com.sourabh.user_service.dto.request;

import com.sourabh.user_service.entity.Role;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for new user registration.
 *
 * <p>Contains all fields required to create a user account. Validated
 * via Bean Validation constraints; a {@code MethodArgumentNotValidException}
 * is raised (HTTP 400) if any constraint is violated.</p>
 */
@Getter
@Setter
public class RegisterRequest {

    /** User's first name (1–50 characters). */
    @NotBlank
    @Size(max = 50)
    private String firstName;

    /** User's last name (1–50 characters). */
    @NotBlank
    @Size(max = 50)
    private String lastName;

    /** Unique email address used as the login identifier (max 100 characters). */
    @Email
    @NotBlank
    @Size(max = 100)
    private String email;

    /** Plain-text password (8–100 characters); hashed before persistence. */
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    /** Optional 10-digit phone number. */
    @Pattern(regexp = "^[0-9]{10}$",
            message = "Phone number must be 10 digits")
    private String phoneNumber;

    /** Role the user is registering as (BUYER, SELLER, or ADMIN). */
    @NotNull
    private Role role;
}
