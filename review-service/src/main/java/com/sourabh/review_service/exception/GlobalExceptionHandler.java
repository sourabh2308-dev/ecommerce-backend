package com.sourabh.review_service.exception;

import com.sourabh.review_service.common.ErrorResponse;
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
 * Centralised exception handler for the review-service.
 *
 * <p>Intercepts all exceptions thrown from controller methods and converts
 * them into a uniform {@link ErrorResponse} JSON structure. This prevents
 * internal stack traces from leaking to API consumers and ensures a
 * consistent error contract across every endpoint.
 *
 * <h3>Exception &rarr; HTTP status mapping</h3>
 * <table>
 *   <tr><th>Exception</th><th>HTTP status</th><th>Error code</th></tr>
 *   <tr><td>{@link ReviewException}</td><td>400</td><td>REVIEW_ERROR</td></tr>
 *   <tr><td>{@link ReviewAlreadyExistsException}</td><td>409</td><td>REVIEW_ALREADY_EXISTS</td></tr>
 *   <tr><td>{@link ReviewAccessException}</td><td>403</td><td>REVIEW_ACCESS_DENIED</td></tr>
 *   <tr><td>{@link ReviewNotFoundException}</td><td>404</td><td>REVIEW_NOT_FOUND</td></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td><td>400</td><td>VALIDATION_ERROR</td></tr>
 *   <tr><td>{@link Exception} (catch-all)</td><td>500</td><td>INTERNAL_SERVER_ERROR</td></tr>
 * </table>
 *
 * @see ErrorResponse
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles generic review business-rule violations.
     *
     * @param ex the caught {@link ReviewException}
     * @return {@code 400 Bad Request} response with error details
     */
    @ExceptionHandler(ReviewException.class)
    public ResponseEntity<ErrorResponse> handleReviewException(ReviewException ex) {
        return buildError("REVIEW_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles duplicate-review attempts (one review per buyer per product).
     *
     * @param ex the caught {@link ReviewAlreadyExistsException}
     * @return {@code 409 Conflict} response with error details
     */
    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(ReviewAlreadyExistsException ex) {
        return buildError("REVIEW_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Handles unauthorised access to review resources.
     *
     * @param ex the caught {@link ReviewAccessException}
     * @return {@code 403 Forbidden} response with error details
     */
    @ExceptionHandler(ReviewAccessException.class)
    public ResponseEntity<ErrorResponse> handleAccess(ReviewAccessException ex) {
        return buildError("REVIEW_ACCESS_DENIED", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Handles review-not-found lookups.
     *
     * @param ex the caught {@link ReviewNotFoundException}
     * @return {@code 404 Not Found} response with error details
     */
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ReviewNotFoundException ex) {
        return buildError("REVIEW_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles Jakarta Bean Validation failures triggered by {@code @Valid}
     * on controller method parameters.
     *
     * <p>Collects all field-level errors and returns them in the
     * {@link ErrorResponse#getDetails()} list (e.g.&nbsp;{@code "rating :
     * must be at least 1"}).
     *
     * @param ex the caught {@link MethodArgumentNotValidException}
     * @return {@code 400 Bad Request} response with per-field error details
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
     * Catch-all handler for any unexpected exception not matched by the
     * more specific handlers above. Logs the full stack trace at
     * {@code ERROR} level for server-side debugging.
     *
     * @param ex the caught {@link Exception}
     * @return {@code 500 Internal Server Error} response with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError("INTERNAL_SERVER_ERROR", "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Builds a standardised {@link ErrorResponse} wrapped in a
     * {@link ResponseEntity} with the specified HTTP status.
     *
     * @param code    machine-readable error code (e.g.&nbsp;{@code REVIEW_NOT_FOUND})
     * @param message human-readable description of the error
     * @param status  the HTTP status to return
     * @return the fully constructed {@link ResponseEntity}
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
