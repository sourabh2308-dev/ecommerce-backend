package com.sourabh.product_service.exception;

/**
 * Thrown when a product operation is invalid given the product's current state.
 *
 * <p>Examples include attempting to purchase a deactivated product or updating
 * a product that has been soft-deleted.  The {@link GlobalExceptionHandler}
 * maps this exception to an HTTP {@code 400 Bad Request} response with error
 * code {@code INVALID_PRODUCT_STATE}.
 *
 * @see GlobalExceptionHandler#handleState(ProductStateException)
 */
public class ProductStateException extends RuntimeException {

    /**
     * Constructs a new {@code ProductStateException}.
     *
     * @param message descriptive text explaining the invalid state transition
     */
    public ProductStateException(String message) {
        super(message);
    }
}
