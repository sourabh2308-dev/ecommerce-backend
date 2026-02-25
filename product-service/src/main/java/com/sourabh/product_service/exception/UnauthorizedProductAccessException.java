package com.sourabh.product_service.exception;

// Custom Exception - Domain-specific error handling
public class UnauthorizedProductAccessException extends RuntimeException {

    public UnauthorizedProductAccessException(String message) {
        super(message);
    }
}
