package com.sourabh.auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * DATA TRANSFER OBJECT (DTO) - Client Request Format
 * 
 * Defines the JSON structure expected from HTTP clients.
 * @NotNull/@NotBlank: Validation constraints (checked by @Valid)
 * 
 * Separation of concerns:
 *   - Entity: Database representation
 *   - Request DTO: HTTP request format (may differ from Entity)
 *   - Response DTO: HTTP response format
 */
public class LoginRequest {

    @Email
    /**

     * VALIDATION: This field is required and cannot be blank.

     * @NotBlank checks: not null, not empty string, not whitespace-only.

     * Triggers MethodArgumentNotValidException if violated (returns 400 Bad Request).

     */

    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String email;

    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String password;
}
