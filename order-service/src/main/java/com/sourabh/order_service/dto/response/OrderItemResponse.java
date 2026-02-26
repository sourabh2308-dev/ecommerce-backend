package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

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
public class OrderItemResponse {

    private String productUuid;
    private String productName;
    private String productCategory;
    private String productImageUrl;
    private String sellerUuid;
    private Double price;
    private Integer quantity;
}
