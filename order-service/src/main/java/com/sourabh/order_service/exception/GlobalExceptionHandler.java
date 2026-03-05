package com.sourabh.order_service.exception;

import com.sourabh.order_service.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Centralised exception handler for the order-service that intercepts
 * exceptions thrown by any {@code @RestController} and converts them into
 * a uniform {@link ErrorResponse} JSON structure.
 *
 * <p>Each {@link ExceptionHandler} method maps a specific exception type
 * to the appropriate HTTP status code, ensuring that stack traces are never
 * leaked to API consumers and that error responses remain consistent.</p>
 *
 * @see ErrorResponse
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link OrderNotFoundException} and returns a {@code 404 Not Found}
     * response with error code {@code ORDER_NOT_FOUND}.
     *
     * @param ex the thrown exception
     * @return a {@link ResponseEntity} containing the error details
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {
        return buildError("ORDER_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles {@link OrderAccessException} and returns a {@code 403 Forbidden}
     * response with error code {@code ORDER_ACCESS_DENIED}.
     *
     * @param ex the thrown exception
     * @return a {@link ResponseEntity} containing the error details
     */
    @ExceptionHandler(OrderAccessException.class)
    public ResponseEntity<ErrorResponse> handleAccess(OrderAccessException ex) {
        return buildError("ORDER_ACCESS_DENIED", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Handles {@link OrderStateException} and returns a {@code 400 Bad Request}
     * response with error code {@code ORDER_INVALID_STATE}.
     *
     * @param ex the thrown exception
     * @return a {@link ResponseEntity} containing the error details
     */
    @ExceptionHandler(OrderStateException.class)
    public ResponseEntity<ErrorResponse> handleState(OrderStateException ex) {
        return buildError("ORDER_INVALID_STATE", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles bean-validation failures triggered by {@code @Valid} and
     * returns a {@code 400 Bad Request} response containing the list of
     * per-field validation error messages.
     *
     * @param ex the validation exception containing binding result errors
     * @return a {@link ResponseEntity} with detailed field-level error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
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

    /**
     * Catch-all handler for any unhandled exception. Logs the full stack trace
     * for server-side diagnostics and returns a generic {@code 500 Internal
     * Server Error} response.
     *
     * @param ex the unhandled exception
     * @return a {@link ResponseEntity} with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError("INTERNAL_SERVER_ERROR", "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper method that constructs a standardised {@link ErrorResponse}
     * wrapped in a {@link ResponseEntity} with the given HTTP status.
     *
     * @param code    application-specific error code
     * @param message human-readable error description
     * @param status  the HTTP status to return
     * @return a {@link ResponseEntity} containing the built {@link ErrorResponse}
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
