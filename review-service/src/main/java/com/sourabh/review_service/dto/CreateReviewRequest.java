package com.sourabh.review_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateReviewRequest {

    /**


     * VALIDATION: This field is required and cannot be blank.


     * @NotBlank checks: not null, not empty string, not whitespace-only.


     * Triggers MethodArgumentNotValidException if violated (returns 400 Bad Request).


     */


    @NotBlank(message = "Order UUID is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String orderUuid;

    @NotBlank(message = "Product UUID is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String productUuid;

    @NotNull(message = "Rating is required")
    // Validation constraint
    // @NotNull - Validates input before processing
    @Min(value = 1, message = "Rating must be at least 1")
    // Validation constraint
    // @Min - Validates input before processing
    @Max(value = 5, message = "Rating must not exceed 5")
    // Validation constraint
    // @Max - Validates input before processing
    // Validation constraint
    // @Max - Validates input before processing
    private Integer rating;

    private String comment;
}
