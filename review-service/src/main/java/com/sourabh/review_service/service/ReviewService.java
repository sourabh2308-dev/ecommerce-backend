package com.sourabh.review_service.service;

import com.sourabh.review_service.common.PageResponse;
import com.sourabh.review_service.dto.CreateReviewRequest;
import com.sourabh.review_service.dto.ReviewResponse;
import com.sourabh.review_service.dto.UpdateReviewRequest;

public interface ReviewService {

    ReviewResponse createReview(CreateReviewRequest request, String buyerUuid);

    PageResponse<ReviewResponse> getReviewsByProduct(String productUuid, int page, int size);

    ReviewResponse getReviewByUuid(String reviewUuid);

    ReviewResponse updateReview(String reviewUuid, UpdateReviewRequest request, String buyerUuid);

    String deleteReview(String reviewUuid, String role, String buyerUuid);

    PageResponse<ReviewResponse> getMyReviews(String buyerUuid, int page, int size);
}

