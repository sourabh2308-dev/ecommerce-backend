package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for verifying a One-Time Password (OTP) sent to
 * the user's email during registration or password-reset flows.
 */
@Getter
@Setter
public class VerifyOTPRequest {

    /** Email address that the OTP was delivered to. */
    @NotBlank
    private String email;

    /** The OTP code entered by the user. */
    @NotBlank
    private String otpCode;
}
