package com.sourabh.product_service.kafka.consumer;

import com.sourabh.product_service.kafka.event.ReviewSubmittedEvent;
import com.sourabh.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for review-related events published by the
 * review-service and updates product rating aggregates accordingly.
 * <p>
 * Subscribes to the {@code review.submitted} topic within the
 * {@code product-service} consumer group. When a {@link ReviewSubmittedEvent}
 * is received, the consumer delegates to {@link ProductService#updateRating}
 * to recalculate the product's average rating and total review count.
 * </p>
 * <p>
 * The consumer must be idempotent because Kafka guarantees at-least-once
 * delivery—duplicate events may be delivered after retries or rebalances.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    /** Product service used to persist updated rating aggregates. */
    private final ProductService productService;

    /**
     * Handles an incoming {@link ReviewSubmittedEvent} from the
     * {@code review.submitted} Kafka topic.
     * <p>
     * Extracts the product UUID and rating from the event and updates
     * the product's average rating. Processing is logged for observability.
     * </p>
     *
     * @param event the deserialized review submission event
     */
    @KafkaListener(topics = "review.submitted", groupId = "product-service")
    public void handleReviewSubmitted(ReviewSubmittedEvent event) {
        log.info("Received ReviewSubmittedEvent: productUuid={}, rating={}",
                event.getProductUuid(), event.getRating());
        productService.updateRating(event.getProductUuid(), event.getRating());
    }
}
