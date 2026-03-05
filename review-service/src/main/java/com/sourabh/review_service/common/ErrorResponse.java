package com.sourabh.review_service.common;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error-response DTO returned by
 * {@link com.sourabh.review_service.exception.GlobalExceptionHandler} whenever
 * an unhandled or domain-specific exception is raised within the review-service.
 *
 * <p>Every error response follows a uniform JSON shape so that API consumers
 * (the frontend, API Gateway, or other microservices) can rely on a single
 * contract for error handling across all review endpoints.
 *
 * <h3>Example JSON</h3>
 * <pre>{@code
 * {
 *   "errorCode"  : "REVIEW_NOT_FOUND",
 *   "message"    : "Review not found: rev-abc123",
 *   "details"    : null,
 *   "timestamp"  : "2026-03-05T14:22:10"
 * }
 * }</pre>
 *
 * @see com.sourabh.review_service.exception.GlobalExceptionHandler
 */
@Getter
@Builder
public class ErrorResponse {

    /**
     * Machine-readable error code that identifies the category of the failure.
     * Examples: {@code REVIEW_NOT_FOUND}, {@code VALIDATION_ERROR},
     * {@code REVIEW_ALREADY_EXISTS}, {@code INTERNAL_SERVER_ERROR}.
     */
    private String errorCode;

    /**
     * Human-readable description of what went wrong, suitable for displaying
     * in client-side error dialogs or logging on the consumer side.
     */
    private String message;

    /**
     * Optional list of field-level validation error messages populated when a
     * {@link org.springframework.web.bind.MethodArgumentNotValidException} is
     * caught (e.g. {@code "rating : must be at least 1"}).
     */
    private List<String> details;

    /**
     * Server timestamp ({@link LocalDateTime}) recording when the error was
     * generated, useful for correlating errors with server-side logs.
     */
    private LocalDateTime timestamp;
}
