package com.sourabh.auth_service.exception;

import com.sourabh.auth_service.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for the auth-service.
 *
 * <p>Intercepts exceptions thrown by controllers and services and converts
 * them into a uniform {@link ErrorResponse} JSON structure.  This prevents
 * stack traces from leaking to API clients and guarantees a consistent
 * error format across all endpoints.</p>
 *
 * <p>Exception-to-HTTP-status mapping:</p>
 * <ul>
 *   <li>{@link AuthException} &rarr; 401 Unauthorized</li>
 *   <li>{@link UserAccountException} &rarr; 403 Forbidden</li>
 *   <li>{@link UserNotFoundException} &rarr; 404 Not Found</li>
 *   <li>{@link MethodArgumentNotValidException} &rarr; 400 Bad Request</li>
 *   <li>{@link Exception} (catch-all) &rarr; 500 Internal Server Error</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles authentication failures (invalid credentials, token errors).
     *
     * @param ex the thrown {@link AuthException}
     * @return 401 Unauthorized with error details
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        return buildError("AUTH_ERROR", ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles user-account state violations (suspended, email not verified).
     *
     * @param ex the thrown {@link UserAccountException}
     * @return 403 Forbidden with error details
     */
    @ExceptionHandler(UserAccountException.class)
    public ResponseEntity<ErrorResponse> handleUserAccountException(UserAccountException ex) {
        return buildError("USER_ACCOUNT_ERROR", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Handles cases where the target user does not exist in
     * {@code user-service}.
     *
     * @param ex the thrown {@link UserNotFoundException}
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return buildError("USER_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles bean-validation failures triggered by {@code @Valid} on
     * request DTOs.  Collects per-field error messages and returns them in
     * the {@code details} list.
     *
     * @param ex the validation exception containing binding results
     * @return 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Catch-all handler for unexpected exceptions.  Logs the full stack
     * trace for server-side debugging and returns a generic error message
     * to the client.
     *
     * @param ex the unexpected exception
     * @return 500 Internal Server Error with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error in auth-service: {}", ex.getMessage(), ex);
        return buildError("INTERNAL_SERVER_ERROR", "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Builds a standardised {@link ErrorResponse} wrapped in a
     * {@link ResponseEntity} with the given HTTP status.
     *
     * @param code    application-specific error code
     * @param message human-readable error description
     * @param status  HTTP status to return
     * @return the formatted error response entity
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
