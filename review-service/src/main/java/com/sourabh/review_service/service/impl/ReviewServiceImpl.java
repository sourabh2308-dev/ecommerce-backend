package com.sourabh.review_service.service.impl;

import com.sourabh.review_service.common.PageResponse;
import com.sourabh.review_service.dto.CreateReviewRequest;
import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.dto.OrderItemDto;
import com.sourabh.review_service.dto.ReviewResponse;
import com.sourabh.review_service.dto.UpdateReviewRequest;
import com.sourabh.review_service.entity.Review;
import com.sourabh.review_service.exception.ReviewAccessException;
import com.sourabh.review_service.exception.ReviewAlreadyExistsException;
import com.sourabh.review_service.exception.ReviewException;
import com.sourabh.review_service.exception.ReviewNotFoundException;
import com.sourabh.review_service.feign.OrderServiceClient;
import com.sourabh.review_service.kafka.event.ReviewSubmittedEvent;
import com.sourabh.review_service.repository.ReviewRepository;
import com.sourabh.review_service.service.ReviewService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // ─────────────────────────────────────────────────
    // CREATE REVIEW
    // ─────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "reviews", key = "#request.productUuid")
    public ReviewResponse createReview(CreateReviewRequest request, String buyerUuid) {

        OrderDto order = fetchOrder(request.getOrderUuid());

        if (!order.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewAccessException("You can only review orders that belong to you");
        }

        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new ReviewException("Reviews can only be submitted after the order is delivered");
        }

        OrderItemDto item = order.getItems()
                .stream()
                .filter(i -> i.getProductUuid().equals(request.getProductUuid()))
                .findFirst()
                .orElseThrow(() -> new ReviewException(
                        "Product " + request.getProductUuid() + " is not part of this order"));

        if (reviewRepository.existsByProductUuidAndBuyerUuid(request.getProductUuid(), buyerUuid)) {
            throw new ReviewAlreadyExistsException("You have already reviewed this product");
        }

        String reviewUuid = UUID.randomUUID().toString();

        Review review = Review.builder()
                .uuid(reviewUuid)
                .productUuid(request.getProductUuid())
                .sellerUuid(item.getSellerUuid())
                .buyerUuid(buyerUuid)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        log.info("Review saved: productUuid={}, buyerUuid={}, rating={}",
                request.getProductUuid(), buyerUuid, request.getRating());

        kafkaTemplate.send(TOPIC_REVIEW_SUBMITTED,
                new ReviewSubmittedEvent(request.getProductUuid(), request.getRating(), reviewUuid));
        log.info("ReviewSubmittedEvent published: productUuid={}", request.getProductUuid());

        return mapToResponse(review);
    }

    // ─────────────────────────────────────────────────
    // GET REVIEWS BY PRODUCT (paginated)
    // ─────────────────────────────────────────────────

    @Override
    @Cacheable(value = "reviews", key = "#productUuid + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByProduct(String productUuid, int page, int size) {
        log.debug("Fetching reviews for productUuid={}, page={}", productUuid, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByProductUuid(productUuid, pageable);
        return toPageResponse(reviewPage);
    }

    // ─────────────────────────────────────────────────
    // GET REVIEW BY UUID
    // ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByUuid(String reviewUuid) {
        Review review = reviewRepository.findByUuid(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));
        return mapToResponse(review);
    }

    // ─────────────────────────────────────────────────
    // UPDATE REVIEW (comment only)
    // ─────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public ReviewResponse updateReview(String reviewUuid, UpdateReviewRequest request, String buyerUuid) {
        Review review = reviewRepository.findByUuid(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));

        if (!review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewAccessException("You can only edit your own reviews");
        }

        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        reviewRepository.save(review);
        log.info("Review updated: uuid={}", reviewUuid);
        return mapToResponse(review);
    }

    // ─────────────────────────────────────────────────
    // DELETE REVIEW
    // ─────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public String deleteReview(String reviewUuid, String role, String buyerUuid) {
        Review review = reviewRepository.findByUuid(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (!isAdmin && !review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewAccessException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review deleted: uuid={}, by role={}", reviewUuid, role);
        return "Review deleted successfully";
    }

    // ─────────────────────────────────────────────────
    // GET MY REVIEWS (buyer's own reviews, paginated)
    // ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(String buyerUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByBuyerUuid(buyerUuid, pageable);
        return toPageResponse(reviewPage);
    }

    // ─────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────

    private OrderDto fetchOrder(String orderUuid) {
        try {
            return orderServiceClient.getOrder(orderUuid);
        } catch (FeignException.NotFound e) {
            throw new ReviewException("Order not found: " + orderUuid);
        }
    }

    private PageResponse<ReviewResponse> toPageResponse(Page<Review> page) {
        List<ReviewResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<ReviewResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .uuid(review.getUuid())
                .productUuid(review.getProductUuid())
                .sellerUuid(review.getSellerUuid())
                .buyerUuid(review.getBuyerUuid())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
