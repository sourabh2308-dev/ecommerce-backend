package com.sourabh.payment_service.exception;

// Custom Exception - Domain-specific error handling
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
