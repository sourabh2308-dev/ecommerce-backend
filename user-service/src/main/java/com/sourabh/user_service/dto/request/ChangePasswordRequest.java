package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ChangePasswordRequest {

    /**


     * VALIDATION: This field is required and cannot be blank.


     * @NotBlank checks: not null, not empty string, not whitespace-only.


     * Triggers MethodArgumentNotValidException if violated (returns 400 Bad Request).


     */


    @NotBlank(message = "Current password is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String currentPassword;

    @NotBlank(message = "New password is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(min = 8, message = "New password must be at least 8 characters")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String confirmNewPassword;
}
