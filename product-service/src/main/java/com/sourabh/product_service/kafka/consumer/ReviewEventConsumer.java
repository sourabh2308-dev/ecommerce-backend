package com.sourabh.product_service.kafka.consumer;

import com.sourabh.product_service.kafka.event.ReviewSubmittedEvent;
import com.sourabh.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * KAFKA CONSUMER - Event Listener for Event-Driven Processing
 * 
 * Listens to Kafka topics and processes events from other services.
 * Enables cross-service communication without direct HTTP calls.
 * @KafkaListener: Spring automatically subscribes to topic on startup.
 */
public class ReviewEventConsumer {

    private final ProductService productService;

    /**


     * KAFKA EVENT CONSUMER - Async Event Processing


     * 


     * PURPOSE:


     * Subscribes to Kafka topic and processes events asynchronously.


     * Part of event-driven architecture for inter-service communication.


     * 


     * HOW IT WORKS:


     * 1. Spring Kafka polls topic for new messages


     * 2. Deserializes JSON to event object


     * 3. Invokes this method in consumer thread pool


     * 4. Acknowledges message on successful processing


     * 5. On exception, retries or sends to DLQ (Dead Letter Queue)


     * 


     * @KafkaListener annotation parameters:


     * - topics: Topic name(s) to subscribe to


     * - groupId: Consumer group (enables load balancing)


     * - containerFactory: Custom config for concurrency, error handling


     * 


     * EVENTUAL CONSISTENCY:


     * Kafka ensures at-least-once delivery. Method must be idempotent


     * (safe to process same event multiple times).


     * 


     * ERROR HANDLING:


     * - Exceptions trigger retry mechanism (configurable retry count)


     * - Failed messages after retries sent to DLQ topic


     * - Use @Transactional to rollback DB changes on error


     */


    @KafkaListener(topics = "review.submitted", groupId = "product-service")
    /**
     * HANDLEREVIEWSUBMITTED - Method Documentation
     *
     * PURPOSE:
     * This method handles the handleReviewSubmitted operation.
     *
     * PARAMETERS:
     * @param event - ReviewSubmittedEvent value
     *
     * ANNOTATIONS USED:
     * @Transactional - Wraps in database transaction (atomic execution)
     * @KafkaListener - Consumes events from Kafka topic
     *
     */
    public void handleReviewSubmitted(ReviewSubmittedEvent event) {
        log.info("Received ReviewSubmittedEvent: productUuid={}, rating={}",
                event.getProductUuid(), event.getRating());
        productService.updateRating(event.getProductUuid(), event.getRating());
    }
}
