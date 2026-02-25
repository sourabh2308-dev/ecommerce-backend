package com.sourabh.review_service.exception;

// Custom Exception - Domain-specific error handling
public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(String message) { super(message); }
}
