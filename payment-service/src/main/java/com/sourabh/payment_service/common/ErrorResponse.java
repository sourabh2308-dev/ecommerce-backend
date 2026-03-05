package com.sourabh.payment_service.common;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error envelope returned by every REST endpoint on failure.
 *
 * <p>Instances are created exclusively by
 * {@link com.sourabh.payment_service.exception.GlobalExceptionHandler} so that
 * all error responses share the same JSON shape regardless of the originating
 * exception type.
 *
 * <p><b>Example JSON payload:</b>
 * <pre>{@code
 * {
 *   "errorCode":  "PAYMENT_NOT_FOUND",
 *   "message":    "Payment not found: pay-abc-123",
 *   "details":    null,
 *   "timestamp":  "2026-03-05T14:22:01.456"
 * }
 * }</pre>
 *
 * @see com.sourabh.payment_service.exception.GlobalExceptionHandler
 */
@Getter
@Builder
public class ErrorResponse {

    /** Machine-readable error code (e.g. {@code PAYMENT_ERROR}, {@code VALIDATION_ERROR}). */
    private String errorCode;

    /** Human-readable summary of the error suitable for display to end-users. */
    private String message;

    /** Optional list of field-level validation messages when input fails {@code @Valid} checks. */
    private List<String> details;

    /** Server timestamp when the error was generated, serialised in ISO-8601 format. */
    private LocalDateTime timestamp;
}
