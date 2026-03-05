package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Internal-only DTO returned by {@code /api/user/internal/**} endpoints
 * for service-to-service communication (e.g. auth-service login flow).
 *
 * <p><b>SECURITY NOTE:</b> This DTO includes the BCrypt-hashed password
 * so that the auth-service can perform credential verification. It must
 * <em>never</em> be exposed on any public-facing endpoint.</p>
 */
@Getter
@Builder
@Jacksonized
public class InternalUserDto {

    /** Unique user identifier. */
    private String uuid;

    /** User's email address (login identifier). */
    private String email;

    /** BCrypt-hashed password; used by auth-service for credential comparison only. */
    private String password;

    /** User role (e.g. BUYER, SELLER, ADMIN). */
    private String role;

    /** Current account status (e.g. ACTIVE, SUSPENDED). */
    private String status;

    /** Whether the user's email address has been verified via OTP. */
    private boolean emailVerified;
}
