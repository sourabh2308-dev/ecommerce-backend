package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.Role;
import com.sourabh.user_service.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Public-facing response payload representing a user profile.
 *
 * <p>Contains only the fields that are safe to expose to API consumers.
 * Sensitive data such as the hashed password is deliberately excluded;
 * see {@link InternalUserDto} for internal service-to-service use.</p>
 */
@Getter
@Builder
@Jacksonized
public class UserResponse {

    /** Unique public identifier of the user. */
    private String uuid;

    /** User's first name. */
    private String firstName;

    /** User's last name. */
    private String lastName;

    /** User's email address. */
    private String email;

    /** User's phone number (may be {@code null}). */
    private String phoneNumber;

    /** Assigned role (BUYER, SELLER, or ADMIN). */
    private Role role;

    /** Current account status (ACTIVE, SUSPENDED, etc.). */
    private UserStatus status;

    /** Whether the user has verified their email address via OTP. */
    private boolean emailVerified;

    /** Whether a seller account has been approved by an admin. */
    private boolean approved;
}
