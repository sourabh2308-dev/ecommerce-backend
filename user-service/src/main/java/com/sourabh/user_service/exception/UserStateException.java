package com.sourabh.user_service.exception;

/**
 * Thrown when an operation cannot be performed because the user's current
 * state does not allow it (e.g. account is suspended, email not verified,
 * or seller not yet approved).
 *
 * <p>Handled by
 * {@link GlobalExceptionHandler#handleUserState(UserStateException)},
 * which returns HTTP 409 (Conflict) with error code
 * {@code USER_STATE_ERROR}.</p>
 */
public class UserStateException extends RuntimeException {

    /**
     * Constructs a new {@code UserStateException} with the specified
     * detail message.
     *
     * @param message a description of the invalid state transition
     */
    public UserStateException(String message) {
        super(message);
    }
}
