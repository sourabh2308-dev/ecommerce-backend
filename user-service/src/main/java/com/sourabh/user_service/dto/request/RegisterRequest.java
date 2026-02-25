package com.sourabh.user_service.dto.request;

import com.sourabh.user_service.entity.Role;
import jakarta.validation.constraints.*;

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
public class RegisterRequest {

    /**


     * VALIDATION: This field is required and cannot be blank.


     * @NotBlank checks: not null, not empty string, not whitespace-only.


     * Triggers MethodArgumentNotValidException if violated (returns 400 Bad Request).


     */


    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 50)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String firstName;

    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 50)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String lastName;

    @Email
    // Validation constraint
    // @Email - Validates input before processing
    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 100)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String email;

    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(min = 8, max = 100)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String password;

    @Pattern(regexp = "^[0-9]{10}$",
    // @Pattern applied to field below
    // @Pattern applied to field below
            message = "Phone number must be 10 digits")
    private String phoneNumber;

    @NotNull
    // Validation constraint
    // @NotNull - Validates input before processing
    // Validation constraint
    // @NotNull - Validates input before processing
    private Role role;

}
