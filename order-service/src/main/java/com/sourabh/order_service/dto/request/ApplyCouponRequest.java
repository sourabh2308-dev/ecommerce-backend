package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for applying or validating a coupon code.
 */
@Data
public class ApplyCouponRequest {

    @NotBlank
    private String couponCode;
}
