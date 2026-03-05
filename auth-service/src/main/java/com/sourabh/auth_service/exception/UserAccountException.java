package com.sourabh.auth_service.exception;

/**
 * Thrown when a user's account state prevents the requested operation &ndash;
 * for example, a suspended account attempting to log in or an unverified
 * email address.
 *
 * <p>Handled by
 * {@link GlobalExceptionHandler#handleUserAccountException(UserAccountException)},
 * which returns HTTP 403 Forbidden with a {@code USER_ACCOUNT_ERROR}
 * code.</p>
 */
public class UserAccountException extends RuntimeException {

    /**
     * Constructs a {@code UserAccountException} with the given detail
     * message.
     *
     * @param message description of the account-state violation
     */
    public UserAccountException(String message) {
        super(message);
    }
}
