package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Internal-only DTO returned by /api/user/internal/** endpoints.
 *
 * This DTO includes the hashed password so the auth-service can perform
 * BCrypt comparison. It must NEVER be served on a public/external endpoint.
 */
@Getter
@Builder
@Jacksonized
public class InternalUserDto {

    private String uuid;
    private String email;
    private String password;   // BCrypt hash — for auth-service comparison only
    private String role;
    private String status;
    private boolean emailVerified;
}
