package com.sourabh.order_service.exception;

/**
 * Runtime exception thrown when an order with the requested UUID cannot be
 * found in the database.
 *
 * <p>Caught by the {@link GlobalExceptionHandler} and mapped to an HTTP
 * {@code 404 Not Found} response with error code {@code ORDER_NOT_FOUND}.</p>
 *
 * @see GlobalExceptionHandler#handleNotFound(OrderNotFoundException)
 */
public class OrderNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code OrderNotFoundException} with the specified detail message.
     *
     * @param message a human-readable description, typically including the missing UUID
     */
    public OrderNotFoundException(String message) { super(message); }
}
