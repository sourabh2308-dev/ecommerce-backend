package com.sourabh.product_service.exception;

/**
 * Thrown when a user attempts a product operation they are not authorised to
 * perform.
 *
 * <p>Common scenario: a seller tries to modify or delete a product that
 * belongs to another seller.  The {@link GlobalExceptionHandler} maps this
 * exception to an HTTP {@code 403 Forbidden} response with error code
 * {@code UNAUTHORIZED_PRODUCT_ACTION}.
 *
 * @see GlobalExceptionHandler#handleUnauthorized(UnauthorizedProductAccessException)
 */
public class UnauthorizedProductAccessException extends RuntimeException {

    /**
     * Constructs a new {@code UnauthorizedProductAccessException}.
     *
     * @param message descriptive text explaining the access violation
     */
    public UnauthorizedProductAccessException(String message) {
        super(message);
    }
}
