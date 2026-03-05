package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO exposing the details of a flash-deal promotion.
 *
 * <p>Includes the original price, the computed discounted price, and the
 * active time window so that the storefront can display countdown timers
 * and strike-through pricing.
 */
@Data
@Builder
public class FlashDealResponse {

    /** Unique identifier of the flash deal. */
    private String uuid;

    /** UUID of the product the deal applies to. */
    private String productUuid;

    /** Display name of the associated product. */
    private String productName;

    /** Product price before the discount is applied. */
    private Double originalPrice;

    /** Discount percentage (1.0 – 99.0). */
    private Double discountPercent;

    /** Effective price after applying the discount. */
    private Double discountedPrice;

    /** Date-time when the deal becomes active. */
    private LocalDateTime startTime;

    /** Date-time when the deal expires. */
    private LocalDateTime endTime;

    /** Whether the deal is currently active (within its time window). */
    private Boolean isActive;
}
