package com.sourabh.auth_service.exception;

/**
 * Thrown when an authentication operation fails &ndash; for example, invalid
 * credentials, expired or revoked tokens, or password-reset failures.
 *
 * <p>Handled by
 * {@link GlobalExceptionHandler#handleAuthException(AuthException)},
 * which returns HTTP 401 Unauthorized with an {@code AUTH_ERROR} code.</p>
 */
public class AuthException extends RuntimeException {

    /**
     * Constructs an {@code AuthException} with the given detail message.
     *
     * @param message description of the authentication failure
     */
    public AuthException(String message) {
        super(message);
    }
}
