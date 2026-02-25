package com.sourabh.user_service.exception;

// Custom Exception - Domain-specific error handling
public class UserStateException extends RuntimeException {
    public UserStateException(String message) {
        super(message);
    }
}
