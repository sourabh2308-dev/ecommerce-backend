package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class InternalUserDto {

    private String uuid;

    private String email;

    private String password;

    private String role;

    private String status;

    private boolean emailVerified;
}
