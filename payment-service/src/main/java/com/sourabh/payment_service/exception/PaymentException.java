package com.sourabh.payment_service.exception;

/**
 * General-purpose exception for payment business-rule violations such as
 * invalid amounts, unsupported operations, or gateway communication errors.
 *
 * <p>Mapped to <b>HTTP 400 Bad Request</b> by
 * {@link GlobalExceptionHandler#handlePayment}.
 */
public class PaymentException extends RuntimeException {

    /**
     * Constructs a new payment exception with the given detail message.
     *
     * @param message explanation of the business-rule violation
     */
    public PaymentException(String message) { super(message); }
}
