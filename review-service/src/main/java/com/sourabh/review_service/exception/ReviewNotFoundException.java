package com.sourabh.review_service.exception;

// Custom Exception - Domain-specific error handling
public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String message) {
        super(message);
    }
}
