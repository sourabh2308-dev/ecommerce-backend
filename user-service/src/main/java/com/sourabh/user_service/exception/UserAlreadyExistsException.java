package com.sourabh.user_service.exception;

// Custom Exception - Domain-specific error handling
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
