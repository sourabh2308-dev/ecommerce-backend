package com.sourabh.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
public class CreateOrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    // @NotEmpty applied to field below
    // @NotEmpty applied to field below
    @Valid
    // @Valid applied to field below
    // @Valid applied to field below
    private List<OrderItemRequest> items;

    // ── Shipping address ────────────────────────────────
    /**

     * VALIDATION: This field is required and cannot be blank.

     * @NotBlank checks: not null, not empty string, not whitespace-only.

     * Triggers MethodArgumentNotValidException if violated (returns 400 Bad Request).

     */

    @NotBlank(message = "Shipping name is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String shippingName;

    @NotBlank(message = "Shipping address is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String shippingAddress;

    @NotBlank(message = "City is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String shippingCity;

    @NotBlank(message = "State is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String shippingState;

    @NotBlank(message = "Pincode is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String shippingPincode;

    @NotBlank(message = "Phone number is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String shippingPhone;
}
