package com.sourabh.review_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReviewResponse {

    private String uuid;

    private String productUuid;

    private String sellerUuid;

    private String buyerUuid;

    private Integer rating;

    private String comment;

    private boolean verifiedPurchase;

    private List<String> imageUrls;

    private long helpfulCount;

    private long notHelpfulCount;

    private LocalDateTime createdAt;
}
