package com.sourabh.review_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
/**
 * DATA TRANSFER OBJECT (DTO) - Server Response Format
 * 
 * Defines the JSON structure returned to HTTP clients.
 * Built from Entity objects via mapper methods.
 * May include computed fields not in database.
 */
public class ReviewResponse {

    private String uuid;
    private String productUuid;
    private String sellerUuid;
    private String buyerUuid;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
