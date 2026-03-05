package com.sourabh.review_service.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating an existing product review.
 *
 * <p>Submitted by the review's author (buyer) via
 * {@code PUT /api/review/{uuid}}. Currently only the comment text may be
 * modified after a review has been submitted; the rating is immutable.
 *
 * @see com.sourabh.review_service.controller.ReviewController#updateReview
 */
@Getter
@Setter
public class UpdateReviewRequest {

    /**
     * New comment text to replace the existing review comment.
     * If {@code null} the comment is left unchanged.
     */
    private String comment;
}
