package com.sourabh.order_service.exception;

// Custom Exception - Domain-specific error handling
public class OrderAccessException extends RuntimeException {
    public OrderAccessException(String message) { super(message); }
}
