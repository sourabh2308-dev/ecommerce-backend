package com.sourabh.user_service.exception;

/**
 * Thrown when an OTP (One-Time Password) operation fails.
 *
 * <p>Common scenarios include:</p>
 * <ul>
 *   <li>The supplied OTP code does not match the stored value.</li>
 *   <li>The OTP has expired beyond the configured validity window.</li>
 *   <li>An OTP was requested too frequently (rate limit exceeded).</li>
 * </ul>
 *
 * <p>Handled by
 * {@link GlobalExceptionHandler#handleOTPException(OTPException)},
 * which returns HTTP 400 (Bad Request) with error code
 * {@code OTP_ERROR}.</p>
 */
public class OTPException extends RuntimeException {

    /**
     * Constructs a new {@code OTPException} with the specified detail message.
     *
     * @param message a human-readable description of the OTP failure
     */
    public OTPException(String message) {
        super(message);
    }
}
