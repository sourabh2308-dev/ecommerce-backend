package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response payload describing a product variant configuration.
 */
@Data
@Builder
public class VariantResponse {
    private String uuid;
    private String variantName;
    private String variantValue;
    private Double priceOverride;
    private Integer stock;
    private String sku;
    private Boolean isActive;
}
