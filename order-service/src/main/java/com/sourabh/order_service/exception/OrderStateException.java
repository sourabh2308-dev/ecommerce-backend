package com.sourabh.order_service.exception;

// Custom Exception - Domain-specific error handling
public class OrderStateException extends RuntimeException {
    public OrderStateException(String message) { super(message); }
}
