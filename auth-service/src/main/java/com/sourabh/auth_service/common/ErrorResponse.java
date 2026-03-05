package com.sourabh.auth_service.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error-response DTO returned to API clients on failure.
 *
 * <p>Used by {@link com.sourabh.auth_service.exception.GlobalExceptionHandler}
 * to ensure every error response conforms to a consistent JSON structure:</p>
 *
 * <pre>{
 *   "errorCode":  "AUTH_ERROR",
 *   "message":    "Invalid credentials",
 *   "details":    ["field : reason", ...],
 *   "timestamp":  "2026-03-05T10:30:00"
 * }</pre>
 *
 * <p>Built via Lombok {@code @Builder}; fields exposed through
 * {@code @Getter}.</p>
 */
@Getter
@Builder
public class ErrorResponse {

    /** Application-specific error code (e.g.&nbsp;{@code AUTH_ERROR}, {@code VALIDATION_ERROR}). */
    private String errorCode;

    /** Human-readable description of the error. */
    private String message;

    /** Optional list of field-level validation messages. */
    private List<String> details;

    /** Timestamp indicating when the error occurred. */
    private LocalDateTime timestamp;
}
