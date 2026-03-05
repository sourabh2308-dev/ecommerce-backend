package com.sourabh.auth_service.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data-transfer object representing a user fetched from
 * {@code user-service}.
 *
 * <p>Populated by {@link org.springframework.web.client.RestTemplate} when
 * calling user-service internal endpoints.  Used during login to verify
 * credentials, account status, and email-verification state.</p>
 */
@Getter
@Setter
public class UserDto {

    /** Unique identifier of the user (UUID format). */
    private String uuid;

    /** User's email address (login principal). */
    private String email;

    /** BCrypt-hashed password stored in {@code user-service}. */
    private String password;

    /** User role: {@code BUYER}, {@code SELLER}, or {@code ADMIN}. */
    private String role;

    /** Account status: {@code ACTIVE}, {@code SUSPENDED}, {@code PENDING_DETAILS}, etc. */
    private String status;

    /** Whether the user has confirmed their email address. */
    private boolean emailVerified;
}
