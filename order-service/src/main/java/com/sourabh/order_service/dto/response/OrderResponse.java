package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    private String uuid;
    private String buyerUuid;
    private Double totalAmount;
    private String status;
    private String paymentStatus;
}

