package com.sourabh.review_service.service;

import com.sourabh.review_service.common.PageResponse;
import com.sourabh.review_service.dto.CreateReviewRequest;
import com.sourabh.review_service.dto.ReviewResponse;
import com.sourabh.review_service.dto.UpdateReviewRequest;

public interface ReviewService {

    /**
     * Create a new review. Only buyers may call this; the buyer's identity comes
     * from the gateway-forwarded X-User-UUID header (buyerUuid).
     */
    ReviewResponse createReview(CreateReviewRequest request, String buyerUuid);

    /**
     * List paginated reviews for a product (visible to everyone).
     */
    PageResponse<ReviewResponse> getReviewsByProduct(String productUuid, int page, int size);

    /**
     * Get a single review by its UUID.
     */
    ReviewResponse getReviewByUuid(String reviewUuid);

    /**
     * Update the comment on an existing review.
     * Only the review's author (buyer) may update it.
     */
    ReviewResponse updateReview(String reviewUuid, UpdateReviewRequest request, String buyerUuid);

    /**
     * Delete a review.
     * Buyers may delete their own; ADMIN may delete any.
     */
    String deleteReview(String reviewUuid, String role, String buyerUuid);

    /**
     * List all reviews written by the authenticated buyer.
     */
    PageResponse<ReviewResponse> getMyReviews(String buyerUuid, int page, int size);
}


