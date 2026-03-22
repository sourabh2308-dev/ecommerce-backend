package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CouponValidationResponse {

    private boolean valid;

    private String message;

    private Double discountAmount;

    private Double finalAmount;
}
