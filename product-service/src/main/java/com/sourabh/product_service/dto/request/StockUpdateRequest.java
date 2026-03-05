package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for manually adjusting a product's stock level.
 *
 * <p>Used by sellers and administrators to increment or set stock quantities
 * outside the normal order flow.  Each adjustment is recorded as a
 * {@code StockMovement} for auditability.
 */
@Data
public class StockUpdateRequest {

    /** Number of units to add; must be at least 1. */
    @NotNull @Min(1)
    private Integer quantity;

    /** Optional free-text reference for the adjustment (e.g. purchase-order number). */
    private String reference;
}
