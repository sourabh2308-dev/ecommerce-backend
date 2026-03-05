package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating or updating a product variant.
 *
 * <p>Variants represent purchasable options of a product (e.g. size, colour).
 * Each variant carries its own SKU, stock level and optional price override.
 */
@Data
public class VariantRequest {

    /** Variant dimension name (e.g. {@code "Size"}, {@code "Color"}). */
    @NotBlank(message = "Variant name is required (e.g. Size, Color)")
    private String variantName;

    /** Specific value within the dimension (e.g. {@code "XL"}, {@code "Red"}). */
    @NotBlank(message = "Variant value is required (e.g. XL, Red)")
    private String variantValue;

    /** Optional price that overrides the base product price for this variant. */
    private Double priceOverride;

    /** Stock quantity for this variant; must be zero or greater. */
    @NotNull @Min(0)
    private Integer stock;

    /** Stock-keeping unit code; must be unique across the catalogue. */
    @NotBlank
    private String sku;
}
