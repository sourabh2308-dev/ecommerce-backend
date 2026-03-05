package com.sourabh.product_service.exception;

import com.sourabh.product_service.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Centralised exception handler for the product-service.
 *
 * <p>Intercepts all exceptions thrown by controllers and services and converts
 * them into a uniform {@link ErrorResponse} JSON payload.  This prevents raw
 * stack traces from leaking to API consumers and ensures every error response
 * follows the same structure.
 *
 * <h3>Exception &rarr; HTTP status mapping</h3>
 * <table>
 *   <tr><th>Exception</th><th>HTTP Status</th><th>Error Code</th></tr>
 *   <tr><td>{@link ProductNotFoundException}</td><td>404</td><td>PRODUCT_NOT_FOUND</td></tr>
 *   <tr><td>{@link UnauthorizedProductAccessException}</td><td>403</td><td>UNAUTHORIZED_PRODUCT_ACTION</td></tr>
 *   <tr><td>{@link ProductStateException}</td><td>400</td><td>INVALID_PRODUCT_STATE</td></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td><td>400</td><td>VALIDATION_ERROR</td></tr>
 *   <tr><td>{@link Exception} (catch-all)</td><td>500</td><td>INTERNAL_SERVER_ERROR</td></tr>
 * </table>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles product-not-found lookups.
     *
     * @param ex the exception thrown when a product UUID cannot be resolved
     * @return a {@code 404 Not Found} response
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {

        return buildError(
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * Handles unauthorised product access attempts.
     *
     * @param ex the exception thrown when a user lacks permission
     * @return a {@code 403 Forbidden} response
     */
    @ExceptionHandler(UnauthorizedProductAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedProductAccessException ex) {

        return buildError(
                "UNAUTHORIZED_PRODUCT_ACTION",
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    /**
     * Handles invalid product state transitions.
     *
     * @param ex the exception thrown when an operation conflicts with the
     *           product's current state
     * @return a {@code 400 Bad Request} response
     */
    @ExceptionHandler(ProductStateException.class)
    public ResponseEntity<ErrorResponse> handleState(ProductStateException ex) {

        return buildError(
                "INVALID_PRODUCT_STATE",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Handles Bean Validation failures triggered by {@code @Valid} on
     * request DTOs.
     *
     * <p>Each field error is formatted as {@code "fieldName : message"} and
     * added to the {@link ErrorResponse#getDetails()} list.
     *
     * @param ex the validation exception containing binding results
     * @return a {@code 400 Bad Request} response with field-level details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
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

    /**
     * Catch-all handler for any unexpected or uncategorised exception.
     *
     * <p>The actual error is logged at {@code ERROR} level; clients receive
     * a generic message to avoid exposing internal details.
     *
     * @param ex the unexpected exception
     * @return a {@code 500 Internal Server Error} response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage());

        return buildError(
                "INTERNAL_SERVER_ERROR",
                "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Helper that constructs an {@link ErrorResponse} wrapped in a
     * {@link ResponseEntity} with the given HTTP status.
     *
     * @param code    machine-readable error code
     * @param message human-readable error description
     * @param status  HTTP status to return
     * @return the fully-built response entity
     */
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
