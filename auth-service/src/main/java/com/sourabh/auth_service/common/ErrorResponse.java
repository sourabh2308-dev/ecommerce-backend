package com.sourabh.auth_service.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
 * ERROR RESPONSE DTO - Standardized Error Format
 * 
 * PURPOSE:
 * Defines the JSON structure for error responses sent to clients.
 * Used by GlobalExceptionHandler to format all error responses consistently.
 * 
 * FIELDS:
 * - timestamp: When the error occurred (ISO-8601 format)
 * - status: HTTP status code (404, 400, 500, etc.)
 * - error: HTTP status reason phrase ("Not Found", "Bad Request")
 * - message: Human-readable error description
 * - path: Request URI that caused the error
 * - errors: (Optional) List of field-level validation errors
 * 
 * EXAMPLE JSON:
 * {
 *   "timestamp": "2026-02-25T10:30:00.123Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Order not found: order-123",
 *   "path": "/api/order/order-123"
 * }
 */
public class ErrorResponse {

    private String errorCode;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;
}
