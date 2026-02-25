package com.sourabh.payment_service.exception;

// Custom Exception - Domain-specific error handling
public class PaymentException extends RuntimeException {
    public PaymentException(String message) { super(message); }
}
