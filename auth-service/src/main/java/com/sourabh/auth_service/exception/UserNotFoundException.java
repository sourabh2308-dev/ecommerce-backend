package com.sourabh.auth_service.exception;

/**
 * Thrown when a user referenced during an authentication operation cannot
 * be found in {@code user-service} &ndash; typically during token refresh
 * when the user may have been deleted after the original login.
 *
 * <p>Handled by
 * {@link GlobalExceptionHandler#handleUserNotFoundException(UserNotFoundException)},
 * which returns HTTP 404 Not Found with a {@code USER_NOT_FOUND} code.</p>
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a {@code UserNotFoundException} with the given detail
     * message.
     *
     * @param message description indicating which user was not found
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
