package com.sourabh.review_service.service;

public interface ReviewService {

    String createReview(
            String orderUuid,
            String productUuid,
            Integer rating,
            String comment,
            String role,
            String buyerUuid);
}

