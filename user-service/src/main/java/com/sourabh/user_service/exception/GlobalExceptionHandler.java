package com.sourabh.user_service.exception;

import com.sourabh.user_service.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

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

    @ExceptionHandler(UserAlreadyExistsException.class)
    /**
     * HANDLEUSEREXISTS - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleUserExists operation.
     *
     * PARAMETERS:
     * @param ex - UserAlreadyExistsException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex) {
        return buildError("USER_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotFoundException.class)
    /**
     * HANDLEUSERNOTFOUND - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleUserNotFound operation.
     *
     * PARAMETERS:
     * @param ex - UserNotFoundException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return buildError("USER_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OTPException.class)
    /**
     * HANDLEOTPEXCEPTION - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleOTPException operation.
     *
     * PARAMETERS:
     * @param ex - OTPException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleOTPException(OTPException ex) {
        return buildError("OTP_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserStateException.class)
    /**
     * HANDLEUSERSTATE - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleUserState operation.
     *
     * PARAMETERS:
     * @param ex - UserStateException value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ErrorResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @ExceptionHandler - Applied to this method
     * @ExceptionHandler - Applied to this method
     *
     */
    public ResponseEntity<ErrorResponse> handleUserState(UserStateException ex) {
        return buildError("USER_STATE_ERROR", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /**
     * HANDLEVALIDATIONEXCEPTION - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleValidationException operation.
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
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> ((FieldError) error).getField() + " : " + error.getDefaultMessage())
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
     * HANDLEGENERALEXCEPTION - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleGeneralException operation.
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
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        return buildError("INTERNAL_SERVER_ERROR",
                "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR);
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
