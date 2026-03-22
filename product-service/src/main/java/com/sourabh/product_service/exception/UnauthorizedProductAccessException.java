package com.sourabh.product_service.exception;

public class UnauthorizedProductAccessException extends RuntimeException {

    public UnauthorizedProductAccessException(String message) {
        super(message);
    }
}
