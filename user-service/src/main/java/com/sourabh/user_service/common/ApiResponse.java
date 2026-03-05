package com.sourabh.user_service.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper used across all user-service endpoints.
 *
 * <p>Provides a uniform JSON envelope so that every successful or failed
 * response shares the same top-level structure, making client-side
 * parsing predictable.</p>
 *
 * <p><b>Example JSON (success):</b></p>
 * <pre>
 * {
 *   "success": true,
 *   "message": "User registered successfully",
 *   "data": { ... },
 *   "timestamp": "2026-03-05T12:30:00"
 * }
 * </pre>
 *
 * @param <T> the type of the response payload carried in {@link #data}
 */
@Getter
@Builder
public class ApiResponse<T> {

    /** Indicates whether the operation completed successfully. */
    private boolean success;

    /** Human-readable message describing the outcome. */
    private String message;

    /** Payload object; may be {@code null} for failure responses. */
    private T data;

    /** Server-side timestamp when the response was created. */
    private LocalDateTime timestamp;

    /**
     * Creates a successful {@link ApiResponse} containing the given payload.
     *
     * @param <T>     the payload type
     * @param message human-readable success message
     * @param data    the response payload
     * @return a fully populated success response with the current timestamp
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a failure {@link ApiResponse} with no payload.
     *
     * @param <T>     the (unused) payload type
     * @param message human-readable failure description
     * @return a failure response with {@code success=false} and the current timestamp
     */
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
