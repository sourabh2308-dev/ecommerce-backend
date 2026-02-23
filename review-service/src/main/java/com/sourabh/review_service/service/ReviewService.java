package com.sourabh.review_service.service;

import com.sourabh.review_service.entity.Review;

import java.util.List;

public interface ReviewService {

    String createReview(
            String orderUuid,
            String productUuid,
            Integer rating,
            String comment,
            String role,
            String buyerUuid);

    List<Review> getReviewsByProduct(String productUuid);
}

