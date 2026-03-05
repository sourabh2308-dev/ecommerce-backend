package com.sourabh.product_service.exception;

/**
 * Thrown when a requested product cannot be found in the database.
 *
 * <p>Typically raised by the service layer when a lookup by UUID yields no
 * result.  The {@link GlobalExceptionHandler} maps this exception to an HTTP
 * {@code 404 Not Found} response with error code {@code PRODUCT_NOT_FOUND}.
 *
 * @see GlobalExceptionHandler#handleNotFound(ProductNotFoundException)
 */
public class ProductNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ProductNotFoundException}.
     *
     * @param message descriptive text, typically including the missing product UUID
     */
    public ProductNotFoundException(String message) {
        super(message);
    }
}
