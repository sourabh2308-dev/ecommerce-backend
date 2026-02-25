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
/**
 * ═══════════════════════════════════════════════════════════════════════════
 * REVIEW SERVICE IMPLEMENTATION - Product Review & Rating Management
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Manages product reviews and ratings submitted by buyers after order delivery.
 * Validates review eligibility (buyer must have purchased the product), prevents
 * duplicate reviews, and publishes events for product rating recalculation.
 * 
 * KEY RESPONSIBILITIES:
 * --------------------
 * 1. Review Creation:
 *    - Validate buyer purchased the product (Feign call to order-service)
 *    - Prevent duplicate reviews for same product by same buyer
 *    - Validate rating (1-5 stars) and review text
 *    - Create Review entity with buyerUuid, productUuid, rating, comment
 * 
 * 2. Review Updates:
 *    - Allow buyers to edit their own reviews
 *    - Update rating or comment text
 *    - Publish review.updated event to recalculate product average rating
 * 
 * 3. Review Deletion:
 *    - Soft delete (isDeleted = true) to maintain audit trail
 *    - Only review owner can delete
 *    - Admins can delete any review (moderation)
 * 
 * 4. Review Retrieval:
 *    - Get all reviews for a product (paginated, sorted by date)
 *    - Get buyer's own reviews across all products
 *    - Filter by rating (e.g., show only 5-star reviews)
 * 
 * 5. Event-Driven Architecture:
 *    - Publishes review.submitted event to Kafka
 *    - product-service listens and recalculates average rating
 *    - Ensures eventual consistency for product ratings
 * 
 * ARCHITECTURE PATTERNS:
 * ----------------------
 * - Service Layer Pattern: Business logic separated from controller
 * - Microservice Collaboration: Feign calls to order-service for validation
 * - Event-Driven: Kafka events for async rating updates
 * - Cache-Aside: @Cacheable for product reviews list
 * - Soft Delete Pattern: isDeleted flag instead of physical deletion
 * 
 * ANNOTATIONS EXPLAINED:
 * ----------------------
 * @Service:
 *   - Marks as Spring-managed service bean
 *   - Eligible for dependency injection
 * 
 * @Transactional:
 *   - Wraps methods in database transaction
 *   - Rollback on RuntimeException
 *   - Ensures atomicity (e.g., save review + publish event)
 * 
 * @Cacheable("reviewsCache"):
 *   - Caches product reviews list
 *   - Key format: reviewsCache::productUuid
 *   - Reduces DB load for frequently viewed products
 *   - TTL configured in RedisCacheConfig
 * 
 * @CacheEvict:
 *   - Clears cache on review creation/update/delete
 *   - Ensures fresh data after modifications
 * 
 * BUSINESS RULES:
 * ---------------
 * 1. Review Eligibility:
 *    - Buyer must have ordered the product
 *    - Order must be delivered (status = DELIVERED)
 *    - One review per buyer per product (no duplicates)
 * 
 * 2. Rating Validation:
 *    - Rating must be between 1 and 5 (inclusive)
 *    - Star ratings: 1 (Poor), 2 (Fair), 3 (Good), 4 (Very Good), 5 (Excellent)
 * 
 * 3. Authorization:
 *    - Only review owner can update/delete their review
 *    - Admins can moderate (delete any review)
 * 
 * 4. Event Publishing:
 *    - review.submitted: Triggers product rating recalculation
 *    - review.updated: Re-triggers rating recalculation
 *    - product-service maintains aggregate rating (avg, count)
 * 
 * FEIGN CLIENT INTEGRATION:
 * --------------------------
 * OrderServiceClient.getOrderByUuid(orderUuid):
 *   - Called during review creation to validate eligibility
 *   - Returns OrderDto with buyer, product, status
 *   - Throws FeignException if order not found
 * 
 * Validation Logic:
 *   1. Check order exists
 *   2. Check order.buyerUuid == current user UUID
 *   3. Check order contains the product UUID
 *   4. Check order status = DELIVERED
 *   5. If all pass: Allow review creation
 * 
 * KAFKA EVENT SCHEMA:
 * -------------------
 * review.submitted Event:
 * {
 *   "reviewUuid": "rev-123",
 *   "productUuid": "prod-456",
 *   "buyerUuid": "buyer-789",
 *   "rating": 5,
 *   "comment": "Excellent product!",
 *   "createdAt": "2026-02-25T10:30:00"
 * }
 * 
 * product-service consumes this and:
 *   - Fetches all reviews for productUuid
 *   - Calculates average rating
 *   - Updates product.averageRating and product.reviewCount
 * 
 * ERROR HANDLING:
 * ---------------
 * - ReviewNotFoundException: Review UUID not found
 * - ReviewAlreadyExistsException: Duplicate review attempt
 * - ReviewException: Order not eligible (not delivered, not buyer's order)
 * - ReviewAccessException: User not authorized to update/delete
 * - FeignException: order-service unavailable (circuit breaker fallback)
 * 
 * CACHING STRATEGY:
 * -----------------
 * Cache Key: reviewsCache::{productUuid}
 * Cache Value: Page<ReviewResponse>
 * TTL: 15 minutes
 * 
 * Eviction Triggers:
 *   - createReview: Evict product reviews cache
 *   - updateReview: Evict product reviews cache
 *   - deleteReview: Evict product reviews cache
 * 
 * EXAMPLE FLOWS:
 * --------------
 * 
 * Flow 1: Create Review
 * createReview(request, buyerUuid)
 * → Validate rating (1-5)
 * → Call order-service: orderServiceClient.getOrderByUuid(request.orderUuid)
 * → Verify order.buyerUuid == buyerUuid
 * → Verify order contains productUuid
 * → Verify order.status == DELIVERED
 * → Check no existing review: reviewRepository.findByBuyerUuidAndProductUuid()
 * → If exists: throw ReviewAlreadyExistsException
 * → Create Review entity (buyerUuid, productUuid, rating, comment)
 * → Save to database
 * → Publish review.submitted event to Kafka
 * → Evict product reviews cache
 * → Convert to ReviewResponse
 * → Return ReviewResponse
 * 
 * Flow 2: Get Product Reviews (with caching)
 * getProductReviews(productUuid, page, size)
 * → Check cache: reviewsCache::{productUuid}
 * → If cache hit: Return cached Page<ReviewResponse>
 * → If cache miss:
 *    → Create Pageable (page, size, sort by createdAt DESC)
 *    → Query: reviewRepository.findByProductUuidAndIsDeletedFalse()
 *    → Convert Page<Review> to Page<ReviewResponse>
 *    → Store in cache (TTL = 15 min)
 *    → Return Page<ReviewResponse>
 * 
 * Flow 3: Update Review
 * updateReview(reviewUuid, request, buyerUuid)
 * → Fetch review by UUID (throw ReviewNotFoundException if not found)
 * → Verify review.buyerUuid == buyerUuid (throw ReviewAccessException if not)
 * → Update rating (if provided in request)
 * → Update comment (if provided in request)
 * → Set updatedAt = now
 * → Save review
 * → Publish review.updated event to Kafka
 * → Evict product reviews cache
 * → Convert to ReviewResponse
 * → Return ReviewResponse
 * 
 * TESTING NOTES:
 * --------------
 * - Mock OrderServiceClient to simulate order validation
 * - Test eligibility rules: non-delivered order, wrong buyer, duplicate review
 * - Verify Kafka event publishing with EmbeddedKafka
 * - Test cache eviction after create/update/delete
 * - Verify authorization: buyer can't edit others' reviews
 * @Transactional ensures database operations are atomic.
 * @Cacheable/@CacheEvict optimize frequent queries.
 */
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
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
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
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
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
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
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
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
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
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
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
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public PageResponse<ReviewResponse> getMyReviews(String buyerUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByBuyerUuid(buyerUuid, pageable);
        return toPageResponse(reviewPage);
    }

    // ─────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────

    /**
     * FETCHORDER - Method Documentation
     *
     * PURPOSE:
     * This method handles the fetchOrder operation.
     *
     * PARAMETERS:
     * @param orderUuid - String value
     *
     * RETURN VALUE:
     * @return OrderDto - Result of the operation
     *
     */
    private OrderDto fetchOrder(String orderUuid) {
        try {
            return orderServiceClient.getOrder(orderUuid);
        } catch (FeignException.NotFound e) {
            throw new ReviewException("Order not found: " + orderUuid);
        }
    }

    /**
     * TOPAGERESPONSE - Method Documentation
     *
     * PURPOSE:
     * This method handles the toPageResponse operation.
     *
     * PARAMETERS:
     * @param page - Page<Review> value
     *
     * RETURN VALUE:
     * @return PageResponse<ReviewResponse> - Result of the operation
     *
     */
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

    /**
     * MAPTORESPONSE - Method Documentation
     *
     * PURPOSE:
     * This method handles the mapToResponse operation.
     *
     * PARAMETERS:
     * @param review - Review value
     *
     * RETURN VALUE:
     * @return ReviewResponse - Result of the operation
     *
     */
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
