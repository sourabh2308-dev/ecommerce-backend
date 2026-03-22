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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    private final ReviewImageRepository reviewImageRepository;

    private final ReviewVoteRepository reviewVoteRepository;

    private final OrderServiceClient orderServiceClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_REVIEW_SUBMITTED = "review.submitted";

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
                .verifiedPurchase(true) 
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByProduct(String productUuid, int page, int size) {
        log.debug("Fetching reviews for productUuid={}, page={}", productUuid, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByProductUuidAndIsDeletedFalse(productUuid, pageable);
        return toPageResponse(reviewPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByUuid(String reviewUuid) {
        Review review = reviewRepository.findByUuidAndIsDeletedFalse(reviewUuid)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewUuid));
        return mapToResponse(review);
    }

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(String buyerUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByBuyerUuidAndIsDeletedFalse(buyerUuid, pageable);
        return toPageResponse(reviewPage);
    }

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
