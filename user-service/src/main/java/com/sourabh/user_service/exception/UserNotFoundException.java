package com.sourabh.user_service.exception;

/**
 * Thrown when a requested user cannot be found by UUID or email.
 *
 * <p>Handled by
 * {@link GlobalExceptionHandler#handleUserNotFound(UserNotFoundException)},
 * which returns HTTP 404 (Not Found) with error code
 * {@code USER_NOT_FOUND}.</p>
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code UserNotFoundException} with the specified
     * detail message.
     *
     * @param message a description indicating which user could not be found
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
