package com.sourabh.user_service.exception;

// Custom Exception - Domain-specific error handling
public class OTPException extends RuntimeException {

    public OTPException(String message) {
        super(message);
    }
}
