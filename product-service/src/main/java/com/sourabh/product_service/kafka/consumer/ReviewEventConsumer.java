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
public class ReviewEventConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "review.submitted", groupId = "product-service")
    public void handleReviewSubmitted(ReviewSubmittedEvent event) {
        log.info("Received ReviewSubmittedEvent: productUuid={}, rating={}",
                event.getProductUuid(), event.getRating());
        productService.updateRating(event.getProductUuid(), event.getRating());
    }
}
