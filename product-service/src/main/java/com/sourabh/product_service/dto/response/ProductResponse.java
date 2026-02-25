package com.sourabh.product_service.dto.response;

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
public class ProductResponse {

    private String uuid;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String category;
    private String sellerUuid;
    private String status;
    private Double averageRating;
    private Integer totalReviews;
    private String imageUrl;
}
