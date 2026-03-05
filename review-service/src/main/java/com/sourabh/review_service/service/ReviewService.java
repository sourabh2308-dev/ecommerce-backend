package com.sourabh.review_service.service;

import com.sourabh.review_service.common.PageResponse;
import com.sourabh.review_service.dto.CreateReviewRequest;
import com.sourabh.review_service.dto.ReviewResponse;
import com.sourabh.review_service.dto.UpdateReviewRequest;

/**
 * Service contract for review management operations.
 *
 * <p>Defines the core CRUD and query methods that the controller layer
 * depends on. The single implementation,
 * {@link com.sourabh.review_service.service.impl.ReviewServiceImpl},
 * handles order verification (via Feign), persistence (via JPA), Kafka
 * event publishing, and Redis cache eviction.
 *
 * @see com.sourabh.review_service.service.impl.ReviewServiceImpl
 */
public interface ReviewService {

    /**
     * Creates a new product review after verifying the buyer's purchase.
     *
     * <p>Validates that the buyer owns the specified order, that the order
     * has been delivered, that the product is part of the order, and that
     * no duplicate review exists. On success a
     * {@code review.submitted} Kafka event is published.
     *
     * @param request   the validated review creation payload
     * @param buyerUuid UUID of the authenticated buyer (from gateway header)
     * @return the newly created review as a {@link ReviewResponse}
     */
    ReviewResponse createReview(CreateReviewRequest request, String buyerUuid);

    /**
     * Returns a paginated list of non-deleted reviews for a given product,
     * sorted by creation date descending.
     *
     * @param productUuid the UUID of the product
     * @param page        zero-based page index
     * @param size        maximum items per page
     * @return a {@link PageResponse} containing the matching reviews
     */
    PageResponse<ReviewResponse> getReviewsByProduct(String productUuid, int page, int size);

    /**
     * Retrieves a single non-deleted review by its UUID.
     *
     * @param reviewUuid the unique identifier of the review
     * @return the matching {@link ReviewResponse}
     * @throws com.sourabh.review_service.exception.ReviewNotFoundException
     *         if no non-deleted review exists with the given UUID
     */
    ReviewResponse getReviewByUuid(String reviewUuid);

    /**
     * Updates the comment text of an existing review. Only the review's
     * original author may perform this operation.
     *
     * @param reviewUuid the UUID of the review to update
     * @param request    the update payload containing the new comment
     * @param buyerUuid  UUID of the authenticated buyer
     * @return the updated {@link ReviewResponse}
     * @throws com.sourabh.review_service.exception.ReviewAccessException
     *         if the caller is not the review's author
     */
    ReviewResponse updateReview(String reviewUuid, UpdateReviewRequest request, String buyerUuid);

    /**
     * Soft-deletes a review. Buyers may delete their own reviews; users
     * with the ADMIN role may delete any review.
     *
     * @param reviewUuid the UUID of the review to delete
     * @param role       the caller's role ({@code BUYER} or {@code ADMIN})
     * @param buyerUuid  UUID of the authenticated user
     * @return a confirmation message
     * @throws com.sourabh.review_service.exception.ReviewAccessException
     *         if a buyer attempts to delete another buyer's review
     */
    String deleteReview(String reviewUuid, String role, String buyerUuid);

    /**
     * Returns a paginated list of reviews authored by the specified buyer,
     * sorted by creation date descending.
     *
     * @param buyerUuid UUID of the buyer whose reviews are requested
     * @param page      zero-based page index
     * @param size      maximum items per page
     * @return a {@link PageResponse} containing the buyer's reviews
     */
    PageResponse<ReviewResponse> getMyReviews(String buyerUuid, int page, int size);
}


