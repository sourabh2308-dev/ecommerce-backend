package com.sourabh.user_service.exception;

// Custom Exception - Domain-specific error handling
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
