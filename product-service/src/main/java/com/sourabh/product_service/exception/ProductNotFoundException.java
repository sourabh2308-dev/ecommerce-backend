package com.sourabh.product_service.exception;

// Custom Exception - Domain-specific error handling
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
