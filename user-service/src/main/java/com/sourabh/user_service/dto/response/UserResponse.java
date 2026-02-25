package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.Role;
import com.sourabh.user_service.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
/**
 * DATA TRANSFER OBJECT (DTO) - Server Response Format
 * 
 * Defines the JSON structure returned to HTTP clients.
 * Built from Entity objects via mapper methods.
 * May include computed fields not in database.
 */
public class UserResponse {

    private String uuid;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private boolean emailVerified;
    private boolean approved;
}
