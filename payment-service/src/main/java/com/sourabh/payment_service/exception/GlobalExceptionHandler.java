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

/**
 * Centralised exception handler that intercepts all controller-level exceptions
 * and converts them into a uniform {@link ErrorResponse} JSON envelope.
 *
 * <p>Each handler method maps a specific exception type to an appropriate HTTP
 * status code and machine-readable error code, ensuring that clients never
 * receive raw stack traces.
 *
 * <p><b>Mapping summary:</b>
 * <ul>
 *   <li>{@link PaymentException}          → 400 Bad Request</li>
 *   <li>{@link PaymentAccessException}    → 403 Forbidden</li>
 *   <li>{@link PaymentNotFoundException}  → 404 Not Found</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 with field-level details</li>
 *   <li>Any other {@link Exception}       → 500 Internal Server Error</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles generic payment business-rule violations.
     *
     * @param ex the thrown {@link PaymentException}
     * @return 400 Bad Request with error details
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex) {
        return buildError("PAYMENT_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles access-denied situations (e.g. buyer trying to view another
     * buyer's payment).
     *
     * @param ex the thrown {@link PaymentAccessException}
     * @return 403 Forbidden with error details
     */
    @ExceptionHandler(PaymentAccessException.class)
    public ResponseEntity<ErrorResponse> handleAccess(PaymentAccessException ex) {
        return buildError("PAYMENT_ACCESS_DENIED", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Handles cases where a requested payment or order UUID does not exist.
     *
     * @param ex the thrown {@link PaymentNotFoundException}
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
        return buildError("PAYMENT_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles bean-validation failures triggered by {@code @Valid} on
     * controller parameters.  Collects all field errors into the
     * {@link ErrorResponse#getDetails()} list.
     *
     * @param ex the validation exception
     * @return 400 Bad Request with per-field error messages
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
     * Catch-all handler for any unhandled exception, preventing stack-trace
     * leakage to external clients.
     *
     * @param ex the unhandled exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError("INTERNAL_SERVER_ERROR", "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper that constructs a standardised {@link ErrorResponse} wrapped in
     * a {@link ResponseEntity} with the given HTTP status.
     *
     * @param code    machine-readable error code
     * @param message human-readable error description
     * @param status  HTTP status to return
     * @return the response entity
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
