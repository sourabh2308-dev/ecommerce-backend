package com.sourabh.review_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published to the {@code review.submitted} topic whenever a
 * new product review is created.
 *
 * <p>The <strong>product-service</strong> consumes this event to
 * recalculate the product's aggregate rating (average and count) so that
 * the product listing stays up-to-date without a synchronous callback.
 *
 * <h3>Example JSON payload</h3>
 * <pre>{@code
 * {
 *   "productUuid" : "prod-xyz789",
 *   "rating"      : 5,
 *   "reviewUuid"  : "rev-abc123"
 * }
 * }</pre>
 *
 * @see com.sourabh.review_service.service.impl.ReviewServiceImpl#createReview
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmittedEvent {

    /** UUID of the product that received the review. */
    private String productUuid;

    /** Star rating (1–5) given by the buyer. */
    private Integer rating;

    /** UUID of the newly created review. */
    private String reviewUuid;
}
