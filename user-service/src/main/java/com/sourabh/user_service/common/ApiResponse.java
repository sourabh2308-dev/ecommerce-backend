package com.sourabh.user_service.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
/**
 * DATA TRANSFER OBJECT (DTO) - Server Response Format
 * 
 * Defines the JSON structure returned to HTTP clients.
 * Built from Entity objects via mapper methods.
 * May include computed fields not in database.
 */
/**
 * API RESPONSE WRAPPER - Generic Success Response Format
 * 
 * PURPOSE:
 * Wraps successful responses in a consistent envelope with status/message.
 * Alternative to returning data directly (more verbose but clearer).
 * 
 * FIELDS:
 * - success: Boolean indicating operation success (true for 2xx responses)
 * - message: Human-readable success message
 * - data: Actual response payload (UserResponse, OrderResponse, etc.)
 * 
 * EXAMPLE JSON:
 * {
 *   "success": true,
 *   "message": "User registered successfully",
 *   "data": {
 *     "uuid": "user-123",
 *     "email": "john@example.com",
 *     "role": "BUYER"
 *   }
 * }
 * 
 * USAGE PATTERN:
 * return ResponseEntity.ok(ApiResponse.success("Operation completed", data));
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    /**
     * SUCCESS - Method Documentation
     *
     * PURPOSE:
     * This method handles the success operation.
     *
     * PARAMETERS:
     * @param message - String value
     * @param data - T value
     *
     * RETURN VALUE:
     * @return static <T> ApiResponse<T> - Result of the operation
     *
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
     * FAILURE - Method Documentation
     *
     * PURPOSE:
     * This method handles the failure operation.
     *
     * PARAMETERS:
     * @param message - String value
     *
     * RETURN VALUE:
     * @return static <T> ApiResponse<T> - Result of the operation
     *
     */
    public static <T> ApiResponse<T> failure(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
