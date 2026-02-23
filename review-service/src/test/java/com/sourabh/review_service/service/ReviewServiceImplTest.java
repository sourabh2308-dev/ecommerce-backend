package com.sourabh.review_service.service;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.dto.OrderItemDto;
import com.sourabh.review_service.entity.Review;
import com.sourabh.review_service.exception.ReviewAccessException;
import com.sourabh.review_service.exception.ReviewAlreadyExistsException;
import com.sourabh.review_service.exception.ReviewException;
import com.sourabh.review_service.feign.OrderServiceClient;
import com.sourabh.review_service.kafka.event.ReviewSubmittedEvent;
import com.sourabh.review_service.repository.ReviewRepository;
import com.sourabh.review_service.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

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

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private OrderDto deliveredOrder;

    @BeforeEach
    void setUp() {
        OrderItemDto item = new OrderItemDto();
        item.setProductUuid("prod-uuid");
        item.setSellerUuid("seller-uuid");

        deliveredOrder = new OrderDto();
        deliveredOrder.setUuid("order-uuid");
        deliveredOrder.setBuyerUuid("buyer-uuid");
        deliveredOrder.setStatus("DELIVERED");
        deliveredOrder.setItems(List.of(item));
    }

    @Test
    @DisplayName("createReview: success — valid buyer, delivered order, new review")
    void createReview_success() {
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);
        when(reviewRepository.existsByProductUuidAndBuyerUuid("prod-uuid", "buyer-uuid")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        String result = reviewService.createReview("order-uuid", "prod-uuid", 5, "Great!", "BUYER", "buyer-uuid");

        assertThat(result).isEqualTo("Review submitted successfully");
        verify(kafkaTemplate).send(eq("review.submitted"), any(ReviewSubmittedEvent.class));
    }

    @Test
    @DisplayName("createReview: fails — role is not BUYER")
    void createReview_notBuyer_throwsReviewAccessException() {
        assertThatThrownBy(() ->
                reviewService.createReview("order-uuid", "prod-uuid", 5, "Great!", "SELLER", "seller-uuid"))
                .isInstanceOf(ReviewAccessException.class)
                .hasMessageContaining("Only buyers");

        verifyNoInteractions(orderServiceClient, kafkaTemplate);
    }

    @Test
    @DisplayName("createReview: fails — order belongs to different buyer")
    void createReview_wrongBuyer_throwsReviewAccessException() {
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);

        assertThatThrownBy(() ->
                reviewService.createReview("order-uuid", "prod-uuid", 5, "Nice", "BUYER", "other-buyer"))
                .isInstanceOf(ReviewAccessException.class)
                .hasMessageContaining("belong to you");
    }

    @Test
    @DisplayName("createReview: fails — order not yet delivered")
    void createReview_notDelivered_throwsReviewException() {
        deliveredOrder.setStatus("CONFIRMED");
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);

        assertThatThrownBy(() ->
                reviewService.createReview("order-uuid", "prod-uuid", 5, "Nice", "BUYER", "buyer-uuid"))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining("after the order is delivered");
    }

    @Test
    @DisplayName("createReview: fails — product not in order")
    void createReview_productNotInOrder_throwsReviewException() {
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);

        assertThatThrownBy(() ->
                reviewService.createReview("order-uuid", "other-prod", 5, "Nice", "BUYER", "buyer-uuid"))
                .isInstanceOf(ReviewException.class)
                .hasMessageContaining("not part of this order");
    }

    @Test
    @DisplayName("createReview: fails — duplicate review")
    void createReview_duplicate_throwsReviewAlreadyExistsException() {
        when(orderServiceClient.getOrder("order-uuid")).thenReturn(deliveredOrder);
        when(reviewRepository.existsByProductUuidAndBuyerUuid("prod-uuid", "buyer-uuid")).thenReturn(true);

        assertThatThrownBy(() ->
                reviewService.createReview("order-uuid", "prod-uuid", 5, "Again!", "BUYER", "buyer-uuid"))
                .isInstanceOf(ReviewAlreadyExistsException.class)
                .hasMessageContaining("already reviewed");

        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("getReviewsByProduct: returns reviews from repository")
    void getReviewsByProduct_returnsResults() {
        Review r = new Review();
        when(reviewRepository.findByProductUuid("prod-uuid")).thenReturn(List.of(r));

        List<Review> reviews = reviewService.getReviewsByProduct("prod-uuid");

        assertThat(reviews).hasSize(1);
    }
}
