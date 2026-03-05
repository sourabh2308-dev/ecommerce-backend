package com.sourabh.user_service.entity;

/**
 * Categorises the purpose for which a one-time password (OTP) was
 * generated.
 * <p>
 * The type is stored alongside the OTP record in
 * {@link OTPVerification} to ensure the correct verification flow
 * is applied (e.g. email verification vs. password reset).
 * </p>
 *
 * @see OTPVerification
 */
public enum OTPType {

    /** OTP sent to verify the user's email address during registration. */
    EMAIL,

    /** OTP sent to verify the user's phone number. */
    PHONE,

    /** OTP sent to authorise a password reset request. */
    PASSWORD_RESET
}
