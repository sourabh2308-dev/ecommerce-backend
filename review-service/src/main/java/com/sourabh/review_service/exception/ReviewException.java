package com.sourabh.review_service.exception;

// Custom Exception - Domain-specific error handling
public class ReviewException extends RuntimeException {
    public ReviewException(String message) { super(message); }
}
