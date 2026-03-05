package com.sourabh.user_service.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error response returned by {@link com.sourabh.user_service.exception.GlobalExceptionHandler}
 * whenever an exception is raised within the user-service.
 *
 * <p>All error responses share this structure so that API clients can rely on
 * a predictable schema for error handling.</p>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * {
 *   "errorCode": "USER_NOT_FOUND",
 *   "message": "No user with the given UUID",
 *   "details": null,
 *   "timestamp": "2026-03-05T12:30:00"
 * }
 * </pre>
 */
@Getter
@Builder
public class ErrorResponse {

    /** Application-specific error code (e.g. {@code USER_NOT_FOUND}, {@code VALIDATION_ERROR}). */
    private String errorCode;

    /** Human-readable description of the error. */
    private String message;

    /** Optional list of field-level validation errors; {@code null} for non-validation errors. */
    private List<String> details;

    /** Server-side timestamp indicating when the error occurred. */
    private LocalDateTime timestamp;
}
