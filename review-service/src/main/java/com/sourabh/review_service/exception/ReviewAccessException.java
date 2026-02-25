package com.sourabh.review_service.exception;

// Custom Exception - Domain-specific error handling
public class ReviewAccessException extends RuntimeException {
    public ReviewAccessException(String message) { super(message); }
}
