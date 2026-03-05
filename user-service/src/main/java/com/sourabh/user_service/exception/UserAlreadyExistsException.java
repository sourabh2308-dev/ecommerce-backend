package com.sourabh.user_service.exception;

/**
 * Thrown when an attempt is made to register a user with an email address
 * that is already associated with an existing account.
 *
 * <p>Handled by
 * {@link GlobalExceptionHandler#handleUserExists(UserAlreadyExistsException)},
 * which returns HTTP 409 (Conflict) with error code
 * {@code USER_ALREADY_EXISTS}.</p>
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new {@code UserAlreadyExistsException} with the specified
     * detail message.
     *
     * @param message a description indicating which identifier already exists
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
