package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCouponRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String discountType;

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

    private String sellerUuid;
}
