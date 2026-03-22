package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Jacksonized
public class OrderResponse {

    private String uuid;

    private String buyerUuid;

    private Double totalAmount;

    private String status;

    private String paymentStatus;

    private List<OrderItemResponse> items;

    private String orderType;

    private String parentOrderUuid;

    private String orderGroupId;

    private String shippingName;

    private String shippingAddress;

    private String shippingCity;

    private String shippingState;

    private String shippingPincode;

    private String shippingPhone;

    private String returnType;

    private String returnReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

