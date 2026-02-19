package com.sourabh.review_service.service.impl;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.dto.OrderItemDto;
import com.sourabh.review_service.entity.Review;
import com.sourabh.review_service.repository.ReviewRepository;
import com.sourabh.review_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;

    private static final String ORDER_SERVICE_URL =
            "http://localhost:8084/api/orders/";

    @Override
    public String createReview(
            String orderUuid,
            String productUuid,
            Integer rating,
            String comment,
            String role,
            String buyerUuid) {

        if (!"BUYER".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only buyers can review");
        }

        // 1️⃣ Fetch order
        OrderDto order = restTemplate.getForObject(
                ORDER_SERVICE_URL + orderUuid,
                OrderDto.class
        );

        if (order == null) {
            throw new RuntimeException("Order not found");
        }

        if (!order.getBuyerUuid().equals(buyerUuid)) {
            throw new RuntimeException("Invalid buyer");
        }

        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException(
                    "Review allowed only after delivery");
        }

        // 2️⃣ Check product part of order
        OrderItemDto item = order.getItems()
                .stream()
                .filter(i ->
                        i.getProductUuid()
                                .equals(productUuid))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not in this order"));

        // 3️⃣ Check duplicate review
        if (reviewRepository.existsByProductUuidAndBuyerUuid(
                productUuid,
                buyerUuid)) {

            throw new RuntimeException(
                    "You already reviewed this product");
        }

        // 4️⃣ Save review
        Review review = Review.builder()
                .uuid(UUID.randomUUID().toString())
                .productUuid(productUuid)
                .sellerUuid(item.getSellerUuid())
                .buyerUuid(buyerUuid)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        String updateRatingUrl =
                "http://localhost:8083/api/products/internal/update-rating/"
                        + productUuid
                        + "?rating=" + rating;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", "veryStrongInternalSecret123");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(
                updateRatingUrl,
                HttpMethod.PUT,
                entity,
                Void.class
        );


        return "Review submitted successfully";
    }
}
