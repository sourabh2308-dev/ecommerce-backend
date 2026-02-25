package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Jacksonized
/**
 * DATA TRANSFER OBJECT (DTO) - Server Response Format
 * 
 * Defines the JSON structure returned to HTTP clients.
 * Built from Entity objects via mapper methods.
 * May include computed fields not in database.
 */
public class OrderResponse {

    private String uuid;
    private String buyerUuid;
    private Double totalAmount;
    private String status;
    private String paymentStatus;
    private List<OrderItemResponse> items;

    // ── Shipping address ────────────────────────────────
    private String shippingName;
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String shippingPhone;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

