package com.sourabh.order_service.exception;

/**
 * Runtime exception thrown when an order operation is attempted but the
 * order's current state does not permit the requested transition (e.g.,
 * attempting to ship an already-cancelled order).
 *
 * <p>Caught by the {@link GlobalExceptionHandler} and mapped to an HTTP
 * {@code 400 Bad Request} response with error code {@code ORDER_INVALID_STATE}.</p>
 *
 * @see GlobalExceptionHandler#handleState(OrderStateException)
 */
public class OrderStateException extends RuntimeException {

    /**
     * Constructs a new {@code OrderStateException} with the specified detail message.
     *
     * @param message a human-readable description of the invalid state transition
     */
    public OrderStateException(String message) { super(message); }
}
