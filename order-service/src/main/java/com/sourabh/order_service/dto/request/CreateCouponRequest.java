package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request payload for creating a new discount coupon.
 */
@Data
public class CreateCouponRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String discountType; // PERCENTAGE or FLAT

    @NotNull @Positive
    private Double discountValue;

    private Double minOrderAmount;

    private Double maxDiscount;

    private Integer totalUsageLimit;

    private Integer perUserLimit;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validUntil;

    /** Optional: limit coupon to a specific seller's products */
    private String sellerUuid;
}
