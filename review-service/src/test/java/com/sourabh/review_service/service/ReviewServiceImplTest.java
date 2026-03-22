package com.sourabh.review_service.service;

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
import com.sourabh.review_service.service.impl.ReviewServiceImpl;
import com.sourabh.review_service.entity.ReviewVote;
import com.sourabh.review_service.repository.ReviewImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl Unit Tests")
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderServiceClient orderServiceClient;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private com.sourabh.review_service.repository.ReviewVoteRepository reviewVoteRepository;
    @Mock private ReviewImageRepository reviewImageRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private OrderDto deliveredOrder;

    private CreateReviewRequest createRequest;

    @BeforeEach
    void setUp() {
        lenient().when(reviewVoteRepository.countHelpfulByReviewId(any())).thenReturn(0L);
        lenient().when(reviewVoteRepository.countNotHelpfulByReviewId(any())).thenReturn(0L);

        OrderItemDto item = new OrderItemDto();
        item.setProductUuid("prod-uuid");
        item.setSellerUuid("seller-uuid");

        deliveredOrder = new OrderDto();
        deliveredOrder.setUuid("order-uuid");
        deliveredOrder.setBuyerUuid("buyer-uuid");
        deliveredOrder.setStatus("DELIVERED");
        deliveredOrder.setItems(List.of(item));

        createRequest = new CreateReviewRequest();
        createRequest.setOrderUuid("order-uuid");
        createRequest.setProductUuid("prod-uuid");
        createRequest.setRating(5);
        createRequest.setComment("Great product!");
    }

    @Test
    @DisplayName("createReview: success — valid buyer, delivered order, new review")
    void createReview_success() {
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);
        when(reviewRepository.existsByProductUuidAndBuyerUuidAndIsDeletedFalse("prod-uuid", "buyer-uuid")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        ReviewResponse result = reviewService.createReview(createRequest, "buyer-uuid");

        assertThat(result).isNotNull();
        assertThat(result.getProductUuid()).isEqualTo("prod-uuid");
        assertThat(result.getRating()).isEqualTo(5);
        verify(kafkaTemplate).send(eq("review.submitted"), any(ReviewSubmittedEvent.class));
    }

    @Test
    @DisplayName("createReview: fails — order belongs to different buyer")
    void createReview_wrongBuyer_throwsReviewAccessException() {
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);

        assertThatThrownBy(() -> reviewService.createReview(createRequest, "other-buyer"))
                .isInstanceOf(ReviewAccessException.class)
                .hasMessageContaining("belong to you");
    }

    @Test
    @DisplayName("createReview: fails — order not yet delivered")
    void createReview_notDelivered_throwsReviewException() {
        deliveredOrder.setStatus("CONFIRMED");
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);

        assertThatThrownBy(() -> reviewService.createReview(createRequest, "buyer-uuid"))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining("after the order is delivered");
    }

    @Test
    @DisplayName("createReview: fails — product not in order")
    void createReview_productNotInOrder_throwsReviewException() {
        createRequest.setProductUuid("other-prod");
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);

        assertThatThrownBy(() -> reviewService.createReview(createRequest, "buyer-uuid"))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining("not part of this order");
    }

    @Test
    @DisplayName("createReview: fails — duplicate review")
    void createReview_duplicate_throwsReviewAlreadyExistsException() {
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);
        when(reviewRepository.existsByProductUuidAndBuyerUuidAndIsDeletedFalse("prod-uuid", "buyer-uuid")).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(createRequest, "buyer-uuid"))
                .isInstanceOf(ReviewAlreadyExistsException.class)
                .hasMessageContaining("already reviewed");

        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("getReviewsByProduct: returns paginated reviews")
    void getReviewsByProduct_returnsPaginatedResults() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        var pageImpl = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);
        when(reviewRepository.findByProductUuidAndIsDeletedFalse(eq("prod-uuid"), any())).thenReturn(pageImpl);

        PageResponse<ReviewResponse> response = reviewService.getReviewsByProduct("prod-uuid", 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getReviewByUuid: success")
    void getReviewByUuid_success() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-uuid")).thenReturn(Optional.of(r));

        ReviewResponse result = reviewService.getReviewByUuid("rev-uuid");

        assertThat(result.getUuid()).isEqualTo("rev-uuid");
    }

    @Test
    @DisplayName("getReviewByUuid: not found throws ReviewNotFoundException")
    void getReviewByUuid_notFound() {
        when(reviewRepository.findByUuidAndIsDeletedFalse("bad-uuid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewByUuid("bad-uuid"))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("updateReview: success — buyer can update own review comment")
    void updateReview_success() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-uuid")).thenReturn(Optional.of(r));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setComment("Updated comment");

        ReviewResponse result = reviewService.updateReview("rev-uuid", req, "buyer-uuid");

        assertThat(result.getComment()).isEqualTo("Updated comment");
    }

    @Test
    @DisplayName("updateReview: fails — wrong buyer")
    void updateReview_wrongBuyer() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-uuid")).thenReturn(Optional.of(r));

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setComment("Hacked!");

        assertThatThrownBy(() -> reviewService.updateReview("rev-uuid", req, "other-buyer"))
                .isInstanceOf(ReviewAccessException.class);
    }

    @Test
    @DisplayName("deleteReview: buyer can delete own review")
    void deleteReview_byBuyer_success() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-uuid")).thenReturn(Optional.of(r));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = reviewService.deleteReview("rev-uuid", "BUYER", "buyer-uuid");

        assertThat(result).contains("deleted");
        verify(reviewRepository).save(any(Review.class));
        assertThat(r.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteReview: admin can delete any review")
    void deleteReview_byAdmin_success() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-uuid")).thenReturn(Optional.of(r));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = reviewService.deleteReview("rev-uuid", "ADMIN", "admin-uuid");

        assertThat(result).contains("deleted");
        verify(reviewRepository).save(any(Review.class));
        assertThat(r.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteReview: fails — buyer tries to delete another's review")
    void deleteReview_wrongBuyer_throwsReviewAccessException() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-uuid")).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reviewService.deleteReview("rev-uuid", "BUYER", "other-buyer"))
                .isInstanceOf(ReviewAccessException.class);
    }

    @Test
    @DisplayName("getMyReviews: returns paginated reviews for the buyer")
    void getMyReviews_returnsResults() {
        Review r = buildReview("rev-uuid", "prod-uuid", "buyer-uuid");
        var pageImpl = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);
        when(reviewRepository.findByBuyerUuidAndIsDeletedFalse(eq("buyer-uuid"), any())).thenReturn(pageImpl);

        PageResponse<ReviewResponse> response = reviewService.getMyReviews("buyer-uuid", 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getBuyerUuid()).isEqualTo("buyer-uuid");
    }

    @Test
    @DisplayName("createReview: rating validation - minimum rating 1")
    void createReview_minimumRating() {
        createRequest.setRating(1);
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);
        when(reviewRepository.existsByProductUuidAndBuyerUuidAndIsDeletedFalse("prod-uuid", "buyer-uuid")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse result = reviewService.createReview(createRequest, "buyer-uuid");

        assertThat(result.getRating()).isEqualTo(1);
    }

    @Test
    @DisplayName("createReview: rating validation - maximum rating 5")
    void createReview_maximumRating() {
        createRequest.setRating(5);
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);
        when(reviewRepository.existsByProductUuidAndBuyerUuidAndIsDeletedFalse("prod-uuid", "buyer-uuid")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse result = reviewService.createReview(createRequest, "buyer-uuid");

        assertThat(result.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("getReviewsByProduct: returns empty page for unknown product")
    void getReviewsByProduct_unknownProduct_empty() {
        var emptyPage = new PageImpl<Review>(List.of(), PageRequest.of(0, 10), 0);
        when(reviewRepository.findByProductUuidAndIsDeletedFalse(eq("unknown-prod"), any())).thenReturn(emptyPage);

        PageResponse<ReviewResponse> response = reviewService.getReviewsByProduct("unknown-prod", 0, 10);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("deleteReview: not found throws ReviewNotFoundException")
    void deleteReview_notFound() {
        when(reviewRepository.findByUuidAndIsDeletedFalse("bad-uuid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview("bad-uuid", "BUYER", "buyer-uuid"))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("getMyReviews: returns empty page when buyer has no reviews")
    void getMyReviews_noReviews() {
        var emptyPage = new PageImpl<Review>(List.of(), PageRequest.of(0, 10), 0);
        when(reviewRepository.findByBuyerUuidAndIsDeletedFalse(eq("buyer-uuid"), any())).thenReturn(emptyPage);

        PageResponse<ReviewResponse> response = reviewService.getMyReviews("buyer-uuid", 0, 10);

        assertThat(response.getContent()).isEmpty();
    }

    private Review buildReview(String uuid, String productUuid, String buyerUuid) {
        return Review.builder()
                .uuid(uuid)
                .productUuid(productUuid)
                .sellerUuid("seller-uuid")
                .buyerUuid(buyerUuid)
                .rating(4)
                .comment("Good")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("voteReview: creates new vote entry")
    void voteReview_newVote_createsEntry() {
        Review review = buildReview("rev-1", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-1")).thenReturn(Optional.of(review));
        when(reviewVoteRepository.findByReviewIdAndVoterUuid(review.getId(), "voter-1"))
                .thenReturn(Optional.empty());
        when(reviewVoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        reviewService.voteReview("rev-1", "voter-1", true);

        verify(reviewVoteRepository).save(any(ReviewVote.class));
    }

    @Test
    @DisplayName("voteReview: updates existing vote when voter changes opinion")
    void voteReview_existingVote_updates() {
        Review review = buildReview("rev-2", "prod-uuid", "buyer-uuid");
        ReviewVote existingVote = ReviewVote.builder()
                .review(review)
                .voterUuid("voter-1")
                .helpful(true)
                .build();
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-2")).thenReturn(Optional.of(review));
        when(reviewVoteRepository.findByReviewIdAndVoterUuid(review.getId(), "voter-1"))
                .thenReturn(Optional.of(existingVote));
        when(reviewVoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        reviewService.voteReview("rev-2", "voter-1", false);

        assertThat(existingVote.isHelpful()).isFalse();
        verify(reviewVoteRepository).save(existingVote);
    }

    @Test
    @DisplayName("voteReview: review not found throws ReviewNotFoundException")
    void voteReview_reviewNotFound_throwsReviewNotFoundException() {
        when(reviewRepository.findByUuidAndIsDeletedFalse("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.voteReview("ghost", "voter-1", true))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("addImageToReview: author adds image successfully")
    void addImageToReview_success_savesImage() {
        Review review = buildReview("rev-3", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-3")).thenReturn(Optional.of(review));
        when(reviewImageRepository.countByReviewId(review.getId())).thenReturn(2);
        when(reviewImageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        reviewService.addImageToReview("rev-3", "buyer-uuid", "http://example.com/img.jpg");

        verify(reviewImageRepository).save(any());
    }

    @Test
    @DisplayName("addImageToReview: non-author throws ReviewAccessException")
    void addImageToReview_wrongBuyer_throwsReviewAccessException() {
        Review review = buildReview("rev-4", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-4")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.addImageToReview("rev-4", "other-buyer", "http://img.jpg"))
                .isInstanceOf(ReviewAccessException.class)
                .hasMessageContaining("your own reviews");
    }

    @Test
    @DisplayName("addImageToReview: exceeding 5-image limit throws ReviewException")
    void addImageToReview_limitExceeded_throwsReviewException() {
        Review review = buildReview("rev-5", "prod-uuid", "buyer-uuid");
        when(reviewRepository.findByUuidAndIsDeletedFalse("rev-5")).thenReturn(Optional.of(review));
        when(reviewImageRepository.countByReviewId(review.getId())).thenReturn(5);

        assertThatThrownBy(() -> reviewService.addImageToReview("rev-5", "buyer-uuid", "http://img6.jpg"))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining("Maximum 5 images");
    }

    @Test
    @DisplayName("addImageToReview: review not found throws ReviewNotFoundException")
    void addImageToReview_reviewNotFound_throwsReviewNotFoundException() {
        when(reviewRepository.findByUuidAndIsDeletedFalse("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addImageToReview("none", "buyer-uuid", "http://img.jpg"))
                .isInstanceOf(ReviewNotFoundException.class);
    }
}
