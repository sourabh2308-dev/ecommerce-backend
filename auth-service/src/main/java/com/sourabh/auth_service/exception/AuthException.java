package com.sourabh.auth_service.exception;

// Custom Exception - Domain-specific error handling
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
