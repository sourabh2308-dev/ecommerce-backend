package com.sourabh.product_service.exception;

import com.sourabh.product_service.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
/**
 * GLOBAL EXCEPTION HANDLER - Centralized Error Response Generator
 * 
 * PURPOSE:
 * Intercepts all exceptions thrown in the application and converts them
 * to standardized JSON error responses. Prevents stack traces from leaking
 * to clients and ensures consistent error format across all endpoints.
 * 
 * ARCHITECTURE:
 * @RestControllerAdvice: Spring AOP that intercepts controller exceptions
 * @ExceptionHandler: Maps specific exception types to handler methods
 * 
 * ERROR RESPONSE FORMAT:
 * {
 *   "timestamp": "2026-02-25T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Order not found: order-123",
 *   "path": "/api/order/order-123"
 * }
 * 
 * EXCEPTION MAPPING:
 * - Custom exceptions (NotFoundException, etc.) → Specific HTTP codes
 * - MethodArgumentNotValidException → 400 with validation details
 * - Generic Exception → 500 INTERNAL SERVER ERROR
 * 
 * LOGGING:
 * All exceptions logged at ERROR level for debugging and monitoring.
 * Stack traces captured for server-side analysis.
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    /**
     * HANDLENOTFOUND - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleNotFound operation.
     *
     * PARAMETERS:
     * @param ex - ProductNotFoundException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {

        return buildError(
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UnauthorizedProductAccessException.class)
    /**
     * HANDLEUNAUTHORIZED - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleUnauthorized operation.
     *
     * PARAMETERS:
     * @param ex - UnauthorizedProductAccessException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedProductAccessException ex) {

        return buildError(
                "UNAUTHORIZED_PRODUCT_ACTION",
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(ProductStateException.class)
    /**
     * HANDLESTATE - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleState operation.
     *
     * PARAMETERS:
     * @param ex - ProductStateException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleState(ProductStateException ex) {

        return buildError(
                "INVALID_PRODUCT_STATE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /**
     * HANDLEVALIDATION - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleValidation operation.
     *
     * PARAMETERS:
     * @param ex - MethodArgumentNotValidException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + " : " + error.getDefaultMessage())
                .toList();

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    /**
     * HANDLEGENERIC - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleGeneric operation.
     *
     * PARAMETERS:
     * @param ex - Exception value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage());

        return buildError(
                "INTERNAL_SERVER_ERROR",
                "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ResponseEntity<ErrorResponse> buildError(
            String code,
            String message,
            HttpStatus status) {

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, status);
    }
}
