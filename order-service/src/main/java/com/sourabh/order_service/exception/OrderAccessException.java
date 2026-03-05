package com.sourabh.order_service.exception;

/**
 * Runtime exception thrown when a user attempts to access or modify an order
 * that they do not own or are not authorised to interact with.
 *
 * <p>Caught by the {@link GlobalExceptionHandler} and mapped to an HTTP
 * {@code 403 Forbidden} response with error code {@code ORDER_ACCESS_DENIED}.</p>
 *
 * @see GlobalExceptionHandler#handleAccess(OrderAccessException)
 */
public class OrderAccessException extends RuntimeException {

    /**
     * Constructs a new {@code OrderAccessException} with the specified detail message.
     *
     * @param message a human-readable description of the access violation
     */
    public OrderAccessException(String message) { super(message); }
}
