package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentSplitResponse {
    private String sellerUuid;
    private String productUuid;
    private Double itemAmount;
    private Double platformFeePercent;
    private Double platformFee;
    private Double deliveryFee;
    private Double sellerPayout;
    private String status;
}
