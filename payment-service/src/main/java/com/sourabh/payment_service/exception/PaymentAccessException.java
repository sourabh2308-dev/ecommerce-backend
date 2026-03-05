package com.sourabh.payment_service.exception;

/**
 * Thrown when an authenticated user attempts to access a payment resource
 * that does not belong to them (e.g. a buyer trying to view another buyer's
 * payment).
 *
 * <p>Mapped to <b>HTTP 403 Forbidden</b> by
 * {@link GlobalExceptionHandler#handleAccess}.
 */
public class PaymentAccessException extends RuntimeException {

    /**
     * Constructs a new access exception with the given detail message.
     *
     * @param message explanation of the access violation
     */
    public PaymentAccessException(String message) { super(message); }
}
