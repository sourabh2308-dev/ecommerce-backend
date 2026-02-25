package com.sourabh.review_service.exception;

// Custom Exception - Domain-specific error handling
/**
 * CUSTOM EXCEPTION - Business Logic Error Handler
 * 
 * PURPOSE:
 * Represents a specific error condition in business logic.
 * Thrown when validation fails or business rules are violated.
 * 
 * FLOW:
 * 1. Service layer detects invalid state/input
 * 2. Throws this exception with descriptive message
 * 3. GlobalExceptionHandler catches it
 * 4. Converts to HTTP response with appropriate status code
 * 
 * HTTP STATUS MAPPING:
 * - NotFoundException → 404 NOT FOUND
 * - AccessException → 403 FORBIDDEN
 * - StateException → 400 BAD REQUEST
 * - ValidationException → 400 BAD REQUEST
 * 
 * EXAMPLE USAGE:
 * throw new OrderNotFoundException("Order not found: " + uuid);
 */
public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(String message) { super(message); }
}
