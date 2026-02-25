package com.sourabh.order_service.exception;

// Custom Exception - Domain-specific error handling
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) { super(message); }
}
