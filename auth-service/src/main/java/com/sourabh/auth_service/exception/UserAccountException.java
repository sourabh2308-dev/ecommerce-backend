package com.sourabh.auth_service.exception;

// Custom Exception - Domain-specific error handling
public class UserAccountException extends RuntimeException {
    public UserAccountException(String message) {
        super(message);
    }
}
