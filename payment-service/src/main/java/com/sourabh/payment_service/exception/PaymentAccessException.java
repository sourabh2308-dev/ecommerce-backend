package com.sourabh.payment_service.exception;

// Custom Exception - Domain-specific error handling
public class PaymentAccessException extends RuntimeException {
    public PaymentAccessException(String message) { super(message); }
}
