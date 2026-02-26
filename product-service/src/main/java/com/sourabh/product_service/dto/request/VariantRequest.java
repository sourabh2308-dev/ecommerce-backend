package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request payload for creating or updating a product variant.
 */
@Data
public class VariantRequest {

    @NotBlank(message = "Variant name is required (e.g. Size, Color)")
    private String variantName;

    @NotBlank(message = "Variant value is required (e.g. XL, Red)")
    private String variantValue;

    private Double priceOverride;

    @NotNull @Min(0)
    private Integer stock;

    @NotBlank
    private String sku;
}
