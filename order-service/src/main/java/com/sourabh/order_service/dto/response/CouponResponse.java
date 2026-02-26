package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload describing coupon configuration and availability.
 */
@Data
@Builder
public class CouponResponse {
    private String code;
    private String discountType;
    private Double discountValue;
    private Double minOrderAmount;
    private Double maxDiscount;
    private Integer totalUsageLimit;
    private Integer usedCount;
    private Integer perUserLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private String sellerUuid;
}
