package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO submitted by administrators to create a new discount coupon.
 *
 * <p>Supports both percentage-based and flat-amount discounts, with optional
 * constraints such as minimum order amount, maximum discount cap, usage
 * limits, and seller-specific scoping.</p>
 */
@Data
public class CreateCouponRequest {

    /** Unique alphanumeric coupon code (must not be blank). */
    @NotBlank
    private String code;

    /** Discount type: {@code "PERCENTAGE"} or {@code "FLAT"}. */
    @NotBlank
    private String discountType;

    /** Discount value (percentage points or flat currency amount; must be positive). */
    @NotNull @Positive
    private Double discountValue;

    /** Optional minimum order total required to use this coupon. */
    private Double minOrderAmount;

    /** Optional maximum discount amount (caps percentage discounts). */
    private Double maxDiscount;

    /** Optional total number of times this coupon may be redeemed across all users. */
    private Integer totalUsageLimit;

    /** Optional maximum number of times a single user may redeem this coupon. */
    private Integer perUserLimit;

    /** Start of the coupon's validity window (required). */
    @NotNull
    private LocalDateTime validFrom;

    /** End of the coupon's validity window (required). */
    @NotNull
    private LocalDateTime validUntil;

    /** Optional seller UUID to restrict the coupon to a specific seller's products. */
    private String sellerUuid;
}
