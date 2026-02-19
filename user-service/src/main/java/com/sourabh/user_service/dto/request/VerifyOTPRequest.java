package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOTPRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String otpCode;
}
