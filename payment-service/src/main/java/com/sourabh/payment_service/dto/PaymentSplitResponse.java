package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
/**
 * DATA TRANSFER OBJECT (DTO) - Server Response Format
 * 
 * Defines the JSON structure returned to HTTP clients.
 * Built from Entity objects via mapper methods.
 * May include computed fields not in database.
 */
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
