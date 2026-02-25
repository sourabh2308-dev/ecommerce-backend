package com.sourabh.payment_service.exception;

import com.sourabh.payment_service.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
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

    @ExceptionHandler(PaymentException.class)
    /**
     * HANDLEPAYMENT - Method Documentation
     *
     * PURPOSE:
     * This method handles the handlePayment operation.
     *
     * PARAMETERS:
     * @param ex - PaymentException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex) {
        return buildError("PAYMENT_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentAccessException.class)
    /**
     * HANDLEACCESS - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleAccess operation.
     *
     * PARAMETERS:
     * @param ex - PaymentAccessException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleAccess(PaymentAccessException ex) {
        return buildError("PAYMENT_ACCESS_DENIED", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    /**
     * HANDLENOTFOUND - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleNotFound operation.
     *
     * PARAMETERS:
     * @param ex - PaymentNotFoundException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
        return buildError("PAYMENT_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
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
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(e -> ((FieldError) e).getField() + " : " + e.getDefaultMessage())
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
     * HANDLEGENERAL - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleGeneral operation.
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
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError("INTERNAL_SERVER_ERROR", "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * BUILDERROR - Method Documentation
     *
     * PURPOSE:
     * This method handles the buildError operation.
     *
     * PARAMETERS:
     * @param code - String value
     * @param message - String value
     * @param status - HttpStatus value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    private ResponseEntity<ErrorResponse> buildError(String code, String message, HttpStatus status) {
        return new ResponseEntity<>(
                ErrorResponse.builder()
                        .errorCode(code)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build(),
                status);
    }
}
