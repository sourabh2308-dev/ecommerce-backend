package com.sourabh.review_service.service.impl;

import com.sourabh.review_service.common.PageResponse;
import com.sourabh.review_service.dto.CreateReviewRequest;
import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.dto.OrderItemDto;
import com.sourabh.review_service.dto.ReviewResponse;
import com.sourabh.review_service.dto.UpdateReviewRequest;
import com.sourabh.review_service.entity.Review;
import com.sourabh.review_service.entity.ReviewImage;
import com.sourabh.review_service.entity.ReviewVote;
import com.sourabh.review_service.exception.ReviewAccessException;
import com.sourabh.review_service.exception.ReviewAlreadyExistsException;
import com.sourabh.review_service.exception.ReviewException;
import com.sourabh.review_service.exception.ReviewNotFoundException;
import com.sourabh.review_service.feign.OrderServiceClient;
import com.sourabh.review_service.kafka.event.ReviewSubmittedEvent;
import com.sourabh.review_service.repository.ReviewImageRepository;
import com.sourabh.review_service.repository.ReviewRepository;
import com.sourabh.review_service.repository.ReviewVoteRepository;
import com.sourabh.review_service.service.ReviewService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link ReviewService}.\n *
 * <p>Manages the full lifecycle of product reviews: creation (with purchase
 * verification), retrieval, update, soft-deletion, helpfulness voting, and
 * image attachment. Cross-cutting concerns handled by this class include:
 *
 * <ul>
 *   <li><strong>Order verification</strong> \u2014 synchronous Feign call to
 *       order-service (with Resilience4j circuit breaker) to confirm that the
 *       buyer has a delivered order containing the reviewed product.</li>
 *   <li><strong>Kafka event publishing</strong> \u2014 a
 *       {@link ReviewSubmittedEvent} is sent to the {@code review.submitted}
 *       topic so that product-service can recalculate aggregate ratings.</li>
 *   <li><strong>Redis caching</strong> \u2014 product review lists are cached;
 *       {@code @CacheEvict} annotations invalidate the cache on mutations.</li>
 *   <li><strong>Transactional integrity</strong> \u2014 all write operations
 *       run inside a database transaction that rolls back on runtime
 *       exceptions.</li>
 * </ul>
 *
 * @see ReviewService
 * @see com.sourabh.review_service.controller.ReviewController
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    /** JPA repository for {@link Review} entities. */
    private final ReviewRepository reviewRepository;

    /** JPA repository for {@link ReviewImage} entities. */
    private final ReviewImageRepository reviewImageRepository;

    /** JPA repository for {@link ReviewVote} entities. */
    private final ReviewVoteRepository reviewVoteRepository;

    /** Feign client used to fetch order details from order-service. */
    private final OrderServiceClient orderServiceClient;

    /** Kafka producer for publishing review domain events. */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Kafka topic name for newly submitted reviews. */
    private static final String TOPIC_REVIEW_SUBMITTED = "review.submitted";

    /**
     * Creates a new product review after performing purchase verification.
     *
     * <p>Validation steps performed before persisting:
     * <ol>
     *   <li>Fetch the order from order-service via Feign.</li>
     *   <li>Verify the order belongs to the requesting buyer.</li>
     *   <li>Verify the order status is {@code DELIVERED}.</li>
     *   <li>Verify the product UUID appears in the order line items.</li>
     *   <li>Verify no existing review by this buyer for this product.</li>
     * </ol>
     *
     * On success the review is saved, and a {@link ReviewSubmittedEvent} is
     * published to Kafka so that product-service can recalculate the
     * product's aggregate rating.
     *
     * @param request   validated review creation payload
     * @param buyerUuid UUID of the authenticated buyer
     * @return the newly created review as a {@link ReviewResponse}
     * @throws ReviewAccessException       if the order does not belong to the buyer
     * @throws ReviewException             if the order is not delivered or the product is not in the order
     * @throws ReviewAlreadyExistsException if the buyer has already reviewed this product
     */
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

        if (reviewRepository.existsByProductUuidAndBuyerUuidAndIsDeletedFalse(request.getProductUuid(), buyerUuid)) {
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
                .verifiedPurchase(true) // always true — we validated DELIVERED order above
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

    /**
     * Returns a paginated list of non-deleted reviews for a product,
     * sorted by {@code createdAt} descending (newest first).
     *
     * <p>This is a read-only operation; no cache eviction is performed.
     *
     * @param productUuid UUID of the product
     * @param page        zero-based page index
     * @param size        maximum items per page
     * @return paginated {@link ReviewResponse} list
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByProduct(String productUuid, int page, int size) {
        log.debug("Fetching reviews for productUuid={}, page={}", productUuid, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByProductUuidAndIsDeletedFalse(productUuid, pageable);
        return toPageResponse(reviewPage);
    }

    /**
     * Retrieves a single non-deleted review by its UUID.
     *
     * @param reviewUuid the unique identifier of the review
     * @return the matching {@link ReviewResponse}
     * @throws ReviewNotFoundException if no non-deleted review exists
     */
    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByUuid(String reviewUuid) {
        Review review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));
        return mapToResponse(review);
    }

    /**
     * Updates the comment of an existing review.
     *
     * <p>Only the original buyer (review author) may update the review.
     * The rating is immutable after creation. Evicts all entries in the
     * {@code reviews} cache to ensure consumers see fresh data.
     *
     * @param reviewUuid UUID of the review to update
     * @param request    payload containing the new comment text
     * @param buyerUuid  UUID of the authenticated buyer
     * @return the updated {@link ReviewResponse}
     * @throws ReviewNotFoundException if the review does not exist
     * @throws ReviewAccessException   if the caller is not the author
     */
    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public ReviewResponse updateReview(String reviewUuid, UpdateReviewRequest request, String buyerUuid) {
        Review review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
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

    /**
     * Soft-deletes a review by setting {@code isDeleted = true}.
     *
     * <p>Buyers may only delete their own reviews. Users with the
     * {@code ADMIN} role may delete any review for moderation purposes.
     * Evicts all entries in the {@code reviews} cache.
     *
     * @param reviewUuid UUID of the review to delete
     * @param role       the caller's role ({@code BUYER} or {@code ADMIN})
     * @param buyerUuid  UUID of the authenticated user
     * @return a confirmation message
     * @throws ReviewNotFoundException if the review does not exist
     * @throws ReviewAccessException   if a non-admin tries to delete another's review
     */
    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public String deleteReview(String reviewUuid, String role, String buyerUuid) {
        Review review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (!isAdmin && !review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewAccessException("You can only delete your own reviews");
        }

        review.setDeleted(true);
        reviewRepository.save(review);
        log.info("Review soft-deleted: uuid={}, by role={}", reviewUuid, role);
        return "Review deleted successfully";
    }

    /**
     * Returns a paginated list of reviews authored by the specified buyer,
     * sorted by {@code createdAt} descending.
     *
     * @param buyerUuid UUID of the buyer
     * @param page      zero-based page index
     * @param size      maximum items per page
     * @return paginated {@link ReviewResponse} list
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(String buyerUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByBuyerUuidAndIsDeletedFalse(buyerUuid, pageable);
        return toPageResponse(reviewPage);
    }

    /**
     * Fetches an order from order-service via the Feign client.
     *
     * <p>Translates a Feign {@code 404 Not Found} response into a
     * domain-specific {@link ReviewException} with a descriptive message.
     *
     * @param orderUuid the UUID of the order to retrieve
     * @return the {@link OrderDto} returned by order-service
     * @throws ReviewException if the order is not found (404)
     */
    private OrderDto fetchOrder(String orderUuid) {
        try {
            return orderServiceClient.getOrder(orderUuid);
        } catch (FeignException.NotFound e) {
            throw new ReviewException("Order not found: " + orderUuid);
        }
    }

    /**
     * Converts a Spring Data {@link Page} of {@link Review} entities into
     * a {@link PageResponse} of {@link ReviewResponse} DTOs.
     *
     * @param page the JPA page result
     * @return the equivalent {@link PageResponse} for the API layer
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
     * Maps a {@link Review} entity to a {@link ReviewResponse} DTO.
     *
     * <p>Includes computed fields: image URLs extracted from the review's
     * {@link ReviewImage} collection, and helpful / not-helpful vote
     * counts queried from {@link ReviewVoteRepository}.
     *
     * @param review the JPA entity to map
     * @return the corresponding API response DTO
     */
    private ReviewResponse mapToResponse(Review review) {
        List<String> imageUrls = review.getImages() != null
                ? review.getImages().stream().map(ReviewImage::getImageUrl).toList()
                : Collections.emptyList();

        long helpfulCount = reviewVoteRepository.countHelpfulByReviewId(review.getId());
        long notHelpfulCount = reviewVoteRepository.countNotHelpfulByReviewId(review.getId());

        return ReviewResponse.builder()
                .uuid(review.getUuid())
                .productUuid(review.getProductUuid())
                .sellerUuid(review.getSellerUuid())
                .buyerUuid(review.getBuyerUuid())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .imageUrls(imageUrls)
                .helpfulCount(helpfulCount)
                .notHelpfulCount(notHelpfulCount)
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Records or updates a helpfulness vote on a review.
     *
     * <p>If the voter has already voted on the review the existing vote is
     * toggled to the new value; otherwise a new {@link ReviewVote} entity
     * is created.
     *
     * @param reviewUuid UUID of the review being voted on
     * @param voterUuid  UUID of the authenticated voter
     * @param helpful    {@code true} for helpful, {@code false} for not helpful
     * @throws ReviewNotFoundException if the review does not exist
     */
    public void voteReview(String reviewUuid, String voterUuid, boolean helpful) {
        Review review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));

        Optional<ReviewVote> existing = reviewVoteRepository.findByReviewIdAndVoterUuid(review.getId(), voterUuid);
        if (existing.isPresent()) {
            existing.get().setHelpful(helpful);
            reviewVoteRepository.save(existing.get());
        } else {
            ReviewVote vote = ReviewVote.builder()
                    .review(review)
                    .voterUuid(voterUuid)
                    .helpful(helpful)
                    .build();
            reviewVoteRepository.save(vote);
        }
        log.info("Vote recorded: reviewUuid={}, voterUuid={}, helpful={}", reviewUuid, voterUuid, helpful);
    }

    /**
     * Attaches an image URL to the buyer's own review.
     *
     * <p>Enforces a maximum of five images per review. Only the review's
     * author may add images; other users receive a
     * {@link ReviewAccessException}.
     *
     * @param reviewUuid UUID of the review to attach the image to
     * @param buyerUuid  UUID of the authenticated buyer
     * @param imageUrl   publicly accessible URL of the image
     * @throws ReviewNotFoundException if the review does not exist
     * @throws ReviewAccessException   if the caller is not the review's author
     * @throws ReviewException         if the 5-image limit is exceeded
     */
    public void addImageToReview(String reviewUuid, String buyerUuid, String imageUrl) {
        Review review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));
        if (!review.getBuyerUuid().equals(buyerUuid)) {
            throw new ReviewAccessException("You can only add images to your own reviews");
        }
        if (reviewImageRepository.countByReviewId(review.getId()) >= 5) {
            throw new ReviewException("Maximum 5 images per review");
        }
        ReviewImage image = ReviewImage.builder()
                .review(review)
                .imageUrl(imageUrl)
                .displayOrder(reviewImageRepository.countByReviewId(review.getId()) + 1)
                .build();
        reviewImageRepository.save(image);
        log.info("Image added to review: reviewUuid={}", reviewUuid);
    }
}
