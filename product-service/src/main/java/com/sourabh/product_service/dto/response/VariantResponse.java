package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO exposing the configuration of a single product variant.
 *
 * <p>A variant represents one purchasable option of a product (e.g. a specific
 * size–colour combination) and carries its own SKU, price override, and stock
 * quantity.
 */
@Data
@Builder
public class VariantResponse {

    /** Unique public identifier of the variant. */
    private String uuid;

    /** Variant dimension name (e.g.&nbsp;{@code "Size"}, {@code "Color"}). */
    private String variantName;

    /** Specific value within the dimension (e.g.&nbsp;{@code "XL"}, {@code "Red"}). */
    private String variantValue;

    /** Price override for this variant; {@code null} means the base product price applies. */
    private Double priceOverride;

    /** Stock quantity available for this specific variant. */
    private Integer stock;

    /** Stock-keeping unit code unique across the catalogue. */
    private String sku;

    /** Whether this variant is currently available for purchase. */
    private Boolean isActive;
}
