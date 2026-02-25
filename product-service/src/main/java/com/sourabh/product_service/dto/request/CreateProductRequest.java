package com.sourabh.product_service.dto.request;

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
public class CreateProductRequest {

    /**


     * VALIDATION: This field is required and cannot be blank.


     * @NotBlank checks: not null, not empty string, not whitespace-only.


     * Triggers MethodArgumentNotValidException if violated (returns 400 Bad Request).


     */


    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 255)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String name;

    @Size(max = 2000)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String description;

    @NotNull
    // Validation constraint
    // @NotNull - Validates input before processing
    @Positive
    // @Positive applied to field below
    // @Positive applied to field below
    private Double price;

    @NotNull
    // Validation constraint
    // @NotNull - Validates input before processing
    @Min(0)
    // Validation constraint
    // @Min - Validates input before processing
    // Validation constraint
    // @Min - Validates input before processing
    private Integer stock;

    @NotBlank
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String category;

    private String imageUrl;
}
