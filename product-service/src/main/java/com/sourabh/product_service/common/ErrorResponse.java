package com.sourabh.product_service.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error response DTO returned by the product-service whenever a
 * request cannot be fulfilled.
 *
 * <p>Instances are built exclusively by
 * {@link com.sourabh.product_service.exception.GlobalExceptionHandler} and
 * serialised to JSON before being sent to the caller.  The uniform structure
 * allows API consumers (front-ends, other micro-services) to parse and
 * display errors consistently.
 *
 * <p>Example JSON payload:
 * <pre>{@code
 * {
 *   "errorCode":  "PRODUCT_NOT_FOUND",
 *   "message":    "Product not found: abc-123",
 *   "details":    null,
 *   "timestamp":  "2026-03-05T14:22:00.000"
 * }
 * }</pre>
 *
 * @see com.sourabh.product_service.exception.GlobalExceptionHandler
 */
@Getter
@Builder
public class ErrorResponse {

    /** Machine-readable error code (e.g.&nbsp;{@code PRODUCT_NOT_FOUND}, {@code VALIDATION_ERROR}). */
    private String errorCode;

    /** Human-readable description of the error suitable for display to the end user. */
    private String message;

    /** Optional list of field-level validation error messages; populated on {@code 400} responses. */
    private List<String> details;

    /** ISO-8601 timestamp indicating when the error occurred. */
    private LocalDateTime timestamp;
}
