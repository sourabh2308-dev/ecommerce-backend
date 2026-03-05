package com.sourabh.product_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event DTO representing a newly submitted product review.
 * <p>
 * Published to the {@code review.submitted} topic by the review-service
 * whenever a buyer submits a review. The product-service consumes this
 * event to update the product's aggregate rating metrics.
 * </p>
 * <p>
 * Lombok's {@code @Data} generates getters, setters, {@code toString},
 * {@code equals}, and {@code hashCode}—sufficient for Jackson JSON
 * (de)serialization used by the Kafka message converter.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmittedEvent {

    /** UUID of the product that was reviewed. */
    private String productUuid;

    /** Numeric rating given by the reviewer (typically 1–5). */
    private Integer rating;

    /** UUID of the review entity, useful for idempotency checks. */
    private String reviewUuid;
}
