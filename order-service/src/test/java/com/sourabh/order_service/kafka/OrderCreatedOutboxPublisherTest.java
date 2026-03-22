package com.sourabh.order_service.kafka;

import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderEventOutbox;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.exception.OrderNotFoundException;
import com.sourabh.order_service.repository.OrderEventOutboxRepository;
import com.sourabh.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreatedOutboxPublisher Unit Tests")
class OrderCreatedOutboxPublisherTest {

    @Mock
    private OrderEventOutboxRepository outboxRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderCreatedOutboxPublisher publisher;

    private OrderEventOutbox outbox;

    private Order order;

    @BeforeEach
    void setUp() {
        outbox = OrderEventOutbox.builder()
                .id(11L)
                .eventId("evt-11")
                .orderUuid("ord-11")
                .topic("order.created")
                .published(false)
                .attemptCount(0)
                .build();

        OrderItem item = OrderItem.builder()
                .productUuid("prod-1")
                .sellerUuid("seller-1")
                .price(100.0)
                .quantity(2)
                .build();

        order = Order.builder()
                .uuid("ord-11")
                .buyerUuid("buyer-1")
                .totalAmount(200.0)
                .items(List.of(item))
                .build();
    }

    @Test
    @DisplayName("publishOutboxRecord: success marks outbox published")
    void publishOutboxRecord_success_marksPublished() {
        when(outboxRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(outbox));
        when(orderRepository.findByUuidAndIsDeletedFalse("ord-11")).thenReturn(Optional.of(order));
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        publisher.publishOutboxRecord(11L);

        assertThat(outbox.isPublished()).isTrue();
        assertThat(outbox.getPublishedAt()).isNotNull();
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getLastAttemptAt()).isNotNull();
        verify(kafkaTemplate).send(eq("order.created"), eq("buyer-1"), any());
    }

    @Test
    @DisplayName("publishOutboxRecord: already published is ignored")
    void publishOutboxRecord_alreadyPublished_noOp() {
        outbox.setPublished(true);
        when(outboxRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(outbox));

        publisher.publishOutboxRecord(11L);

        verifyNoInteractions(orderRepository, kafkaTemplate);
    }

    @Test
    @DisplayName("publishOutboxRecord: missing outbox is ignored")
    void publishOutboxRecord_missingOutbox_noOp() {
        when(outboxRepository.findByIdForUpdate(11L)).thenReturn(Optional.empty());

        publisher.publishOutboxRecord(11L);

        verifyNoInteractions(orderRepository, kafkaTemplate);
    }

    @Test
    @DisplayName("publishOutboxRecord: missing order throws OrderNotFoundException")
    void publishOutboxRecord_missingOrder_throwsOrderNotFoundException() {
        when(outboxRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(outbox));
        when(orderRepository.findByUuidAndIsDeletedFalse("ord-11")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publisher.publishOutboxRecord(11L))
                .isInstanceOf(OrderNotFoundException.class);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("publishOutboxRecord: Kafka failure increments attempts and stays unpublished")
    void publishOutboxRecord_kafkaFailure_staysUnpublished() {
        when(outboxRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(outbox));
        when(orderRepository.findByUuidAndIsDeletedFalse("ord-11")).thenReturn(Optional.of(order));
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        assertThatThrownBy(() -> publisher.publishOutboxRecord(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to publish order-created event");

        assertThat(outbox.isPublished()).isFalse();
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getLastAttemptAt()).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, Object> mockSendResult() {
        return mock(SendResult.class);
    }
}
