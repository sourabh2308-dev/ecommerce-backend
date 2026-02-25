package com.sourabh.product_service.exception;

// Custom Exception - Domain-specific error handling
public class ProductStateException extends RuntimeException {

    public ProductStateException(String message) {
        super(message);
    }
}
