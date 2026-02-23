package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.Role;
import com.sourabh.user_service.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
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
