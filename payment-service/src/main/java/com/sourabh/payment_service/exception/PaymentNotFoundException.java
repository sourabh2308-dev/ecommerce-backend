package com.sourabh.payment_service.exception;

/**
 * Thrown when a payment or order UUID cannot be found in the database.
 *
 * <p>Mapped to <b>HTTP 404 Not Found</b> by
 * {@link GlobalExceptionHandler#handleNotFound}.
 */
public class PaymentNotFoundException extends RuntimeException {

    /**
     * Constructs a new not-found exception with the given detail message.
     *
     * @param message description including the missing identifier
     */
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
