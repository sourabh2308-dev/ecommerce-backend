package com.sourabh.auth_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {

    private String uuid;

    private String email;

    private String password;

    private String role;

    private String status;

    private boolean emailVerified;
}
