package com.sourabh.order_service.common;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error response DTO returned by the
 * {@link com.sourabh.order_service.exception.GlobalExceptionHandler}
 * whenever an exception is raised during request processing.
 *
 * <p>All error responses share this uniform shape so that API consumers can
 * parse failures consistently regardless of the underlying cause.</p>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * {
 *   "errorCode": "ORDER_NOT_FOUND",
 *   "message": "Order not found: order-123",
 *   "details": null,
 *   "timestamp": "2026-02-25T10:30:00.123"
 * }
 * </pre>
 *
 * @see com.sourabh.order_service.exception.GlobalExceptionHandler
 */
@Getter
@Builder
public class ErrorResponse {

    /** Application-specific error code (e.g., {@code ORDER_NOT_FOUND}, {@code VALIDATION_ERROR}). */
    private String errorCode;

    /** Human-readable description of the error. */
    private String message;

    /** Optional list of field-level validation error messages. */
    private List<String> details;

    /** Timestamp indicating when the error occurred. */
    private LocalDateTime timestamp;
}
