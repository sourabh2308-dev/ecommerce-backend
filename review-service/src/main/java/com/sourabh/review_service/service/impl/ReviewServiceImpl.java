package com.sourabh.review_service.service.impl;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.dto.OrderItemDto;
import com.sourabh.review_service.entity.Review;
import com.sourabh.review_service.exception.ReviewAccessException;
import com.sourabh.review_service.exception.ReviewAlreadyExistsException;
import com.sourabh.review_service.exception.ReviewException;
import com.sourabh.review_service.feign.OrderServiceClient;
import com.sourabh.review_service.kafka.event.ReviewSubmittedEvent;
import com.sourabh.review_service.repository.ReviewRepository;
import com.sourabh.review_service.service.ReviewService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderServiceClient orderServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_REVIEW_SUBMITTED = "review.submitted";

    @Override
    @CacheEvict(value = "reviews", key = "#productUuid")
    public String createReview(
            String orderUuid,
            String productUuid,
            Integer rating,
            String comment,
            String role,
            String buyerUuid) {

        if (!"BUYER".equalsIgnoreCase(role)) {
            throw new ReviewAccessException("Only buyers can submit reviews");
        }

        OrderDto order = fetchOrder(orderUuid);

        if (!order.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewAccessException("You can only review orders that belong to you");
        }

        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new ReviewException("Reviews can only be submitted after the order is delivered");
        }

        OrderItemDto item = order.getItems()
                .stream()
                .filter(i -> i.getProductUuid().equals(productUuid))
                .findFirst()
                .orElseThrow(() -> new ReviewException("Product " + productUuid + " is not part of this order"));

        if (reviewRepository.existsByProductUuidAndBuyerUuid(productUuid, buyerUuid)) {
            throw new ReviewAlreadyExistsException("You have already reviewed this product");
        }

        String reviewUuid = UUID.randomUUID().toString();

        Review review = Review.builder()
                .uuid(reviewUuid)
                .productUuid(productUuid)
                .sellerUuid(item.getSellerUuid())
                .buyerUuid(buyerUuid)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        log.info("Review saved: productUuid={}, buyerUuid={}, rating={}", productUuid, buyerUuid, rating);

        // Publish async event — product-service updates its rating independently
        kafkaTemplate.send(TOPIC_REVIEW_SUBMITTED,
                new ReviewSubmittedEvent(productUuid, rating, reviewUuid));

        log.info("ReviewSubmittedEvent published: productUuid={}, rating={}", productUuid, rating);
        return "Review submitted successfully";
    }

    @Override
    @Cacheable(value = "reviews", key = "#productUuid")
    public List<Review> getReviewsByProduct(String productUuid) {
        log.debug("Cache miss for reviews of productUuid={} — fetching from DB", productUuid);
        return reviewRepository.findByProductUuid(productUuid);
    }

    private OrderDto fetchOrder(String orderUuid) {
        try {
            return orderServiceClient.getOrder(orderUuid);
        } catch (FeignException.NotFound e) {
            throw new ReviewException("Order not found: " + orderUuid);
        }
    }
}
