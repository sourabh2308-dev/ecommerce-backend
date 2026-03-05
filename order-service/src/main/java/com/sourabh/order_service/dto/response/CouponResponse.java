package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO describing a discount coupon's configuration, usage
 * statistics, and current availability.
 *
 * <p>Returned when an admin lists, creates, or inspects a coupon.</p>
 */
@Data
@Builder
public class CouponResponse {

    /** Unique alphanumeric coupon code. */
    private String code;

    /** Discount type: {@code "PERCENTAGE"} or {@code "FLAT"}. */
    private String discountType;

    /** Discount value (percentage points or flat currency amount). */
    private Double discountValue;

    /** Minimum order total required to use this coupon (nullable). */
    private Double minOrderAmount;

    /** Maximum discount amount cap for percentage coupons (nullable). */
    private Double maxDiscount;

    /** Total number of times this coupon can be redeemed (nullable). */
    private Integer totalUsageLimit;

    /** How many times the coupon has already been redeemed. */
    private Integer usedCount;

    /** Maximum redemptions allowed per individual user (nullable). */
    private Integer perUserLimit;

    /** Start of the coupon's validity window. */
    private LocalDateTime validFrom;

    /** End of the coupon's validity window. */
    private LocalDateTime validUntil;

    /** Whether the coupon is currently active and redeemable. */
    private Boolean isActive;

    /** UUID of the seller this coupon is restricted to (nullable for platform-wide coupons). */
    private String sellerUuid;
}
