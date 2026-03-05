package com.sourabh.review_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO returned to API consumers for review-related endpoints.
 *
 * <p>Built from the {@link com.sourabh.review_service.entity.Review} entity
 * via the mapping logic in
 * {@link com.sourabh.review_service.service.impl.ReviewServiceImpl}. Includes
 * computed fields such as {@code helpfulCount} and {@code notHelpfulCount}
 * that are derived from {@link com.sourabh.review_service.entity.ReviewVote}
 * aggregates at query time.
 *
 * @see com.sourabh.review_service.controller.ReviewController
 */
@Getter
@Builder
public class ReviewResponse {

    /** Unique identifier of the review. */
    private String uuid;

    /** UUID of the product this review is associated with. */
    private String productUuid;

    /** UUID of the seller who listed the reviewed product. */
    private String sellerUuid;

    /** UUID of the buyer who authored the review. */
    private String buyerUuid;

    /** Star rating (1–5) assigned by the buyer. */
    private Integer rating;

    /** Optional free-text comment written by the buyer. */
    private String comment;

    /** {@code true} if the reviewer had a verified (delivered) purchase. */
    private boolean verifiedPurchase;

    /** Publicly accessible URLs of images attached to this review. */
    private List<String> imageUrls;

    /** Number of users who marked this review as helpful. */
    private long helpfulCount;

    /** Number of users who marked this review as not helpful. */
    private long notHelpfulCount;

    /** Timestamp when the review was originally created. */
    private LocalDateTime createdAt;
}
