package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO returned after validating a coupon code against an order.
 *
 * <p>Indicates whether the coupon is valid and, if so, provides the
 * calculated discount amount and the resulting order total after the
 * discount has been applied.</p>
 */
@Data
@Builder
public class CouponValidationResponse {

    /** {@code true} if the coupon code is valid and applicable to the order. */
    private boolean valid;

    /** Human-readable validation message (e.g., success confirmation or failure reason). */
    private String message;

    /** Calculated discount amount to be subtracted from the order total. */
    private Double discountAmount;

    /** Order total after applying the discount. */
    private Double finalAmount;
}
