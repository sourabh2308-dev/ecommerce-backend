package com.sourabh.review_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {

    private String uuid;
    private String productUuid;
    private String sellerUuid;
    private String buyerUuid;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
