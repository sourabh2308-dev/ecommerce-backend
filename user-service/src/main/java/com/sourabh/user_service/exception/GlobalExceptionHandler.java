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

/**
 * Centralised exception handler for the user-service.
 *
 * <p>Uses {@link RestControllerAdvice} to intercept all exceptions thrown by
 * controllers and convert them into a uniform {@link ErrorResponse} JSON
 * structure. This prevents raw stack traces from leaking to API consumers
 * and guarantees a consistent error format across every endpoint.</p>
 *
 * <h3>Exception &rarr; HTTP status mapping</h3>
 * <table>
 *   <tr><th>Exception</th><th>HTTP Status</th><th>Error Code</th></tr>
 *   <tr><td>{@link UserAlreadyExistsException}</td><td>409 Conflict</td><td>USER_ALREADY_EXISTS</td></tr>
 *   <tr><td>{@link UserNotFoundException}</td><td>404 Not Found</td><td>USER_NOT_FOUND</td></tr>
 *   <tr><td>{@link OTPException}</td><td>400 Bad Request</td><td>OTP_ERROR</td></tr>
 *   <tr><td>{@link UserStateException}</td><td>409 Conflict</td><td>USER_STATE_ERROR</td></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td><td>400 Bad Request</td><td>VALIDATION_ERROR</td></tr>
 *   <tr><td>{@link Exception} (catch-all)</td><td>500 Internal Server Error</td><td>INTERNAL_SERVER_ERROR</td></tr>
 * </table>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles duplicate-user registration attempts.
     *
     * @param ex the caught exception
     * @return HTTP 409 response with error details
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex) {
        return buildError("USER_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Handles requests for users that do not exist.
     *
     * @param ex the caught exception
     * @return HTTP 404 response with error details
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return buildError("USER_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles OTP verification failures (wrong code, expired, rate-limited).
     *
     * @param ex the caught exception
     * @return HTTP 400 response with error details
     */
    @ExceptionHandler(OTPException.class)
    public ResponseEntity<ErrorResponse> handleOTPException(OTPException ex) {
        return buildError("OTP_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles operations that conflict with the user's current state.
     *
     * @param ex the caught exception
     * @return HTTP 409 response with error details
     */
    @ExceptionHandler(UserStateException.class)
    public ResponseEntity<ErrorResponse> handleUserState(UserStateException ex) {
        return buildError("USER_STATE_ERROR", ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Handles bean-validation failures triggered by {@code @Valid} on
     * request DTOs. Each field error is collected into the {@code details}
     * list of the response.
     *
     * @param ex the validation exception containing binding results
     * @return HTTP 400 response with per-field error descriptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
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

    /**
     * Catch-all handler for any unexpected exception not covered by the
     * more specific handlers above. Logs the full stack trace at ERROR level
     * for server-side debugging.
     *
     * @param ex the unhandled exception
     * @return HTTP 500 response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled exception in user-service", ex);
        return buildError("INTERNAL_SERVER_ERROR",
                "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper that builds a standardised {@link ErrorResponse} wrapped in a
     * {@link ResponseEntity} with the given HTTP status.
     *
     * @param code    application-specific error code
     * @param message human-readable error description
     * @param status  the HTTP status to return
     * @return the fully constructed response entity
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
