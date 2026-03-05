package com.sourabh.user_service.kafka.consumer;

import com.sourabh.user_service.entity.NotificationType;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.kafka.event.OrderStatusChangedEvent;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.EmailService;
import com.sourabh.user_service.service.NotificationService;
import com.sourabh.user_service.feign.OrderServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OrderEventConsumer}.
 *
 * <p>Validates the Kafka consumer's behaviour when it receives an
 * {@link OrderStatusChangedEvent} with status transition to {@code DELIVERED}.
 * The consumer is expected to:</p>
 * <ol>
 *   <li>Persist an in-app notification via {@link NotificationService}.</li>
 *   <li>Send a delivery-confirmation HTML email via {@link EmailService}.</li>
 *   <li>Fetch the invoice PDF from order-service and email it as an attachment.</li>
 * </ol>
 *
 * <p>The {@code internalSecret} field (normally injected by Spring) is set
 * via {@link ReflectionTestUtils} since the test runs outside the application context.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer Tests")
class OrderEventConsumerTest {

    /** Mock for persisting in-app notifications. */
    @Mock
    private NotificationService notificationService;

    /** Mock for sending transactional emails. */
    @Mock
    private EmailService emailService;

    /** Mock for user lookups by UUID. */
    @Mock
    private UserRepository userRepository;

    /** Mock Feign client for fetching invoice PDFs from order-service. */
    @Mock
    private OrderServiceClient orderServiceClient;

    /** Consumer under test with mocked dependencies auto-injected. */
    @InjectMocks
    private OrderEventConsumer consumer;

    /** Sample Kafka event representing an order transitioning to DELIVERED. */
    private OrderStatusChangedEvent deliveredEvent;

    /** Sample user entity for the buyer referenced in the event. */
    private User sampleUser;

    /**
     * Initialises test fixtures before each test: a DELIVERED event, a sample
     * user, and the internal-secret field required by the consumer.
     */
    @BeforeEach
    void setUp() {
        deliveredEvent = new OrderStatusChangedEvent("order-1", "buyer-1", "SHIPPED", "DELIVERED", 100.0, "INR");
        sampleUser = new User();
        sampleUser.setUuid("buyer-1");
        sampleUser.setEmail("foo@bar.com");
        sampleUser.setFirstName("Foo");
        org.springframework.test.util.ReflectionTestUtils.setField(consumer, "internalSecret", "test-secret");
    }

    /**
     * Verifies that a DELIVERED event triggers an in-app notification, a
     * delivery-confirmation HTML email, and an invoice attachment email.
     */
    @Test
    @DisplayName("handleOrderStatusChanged should send invoice when delivered")
    void handleDelivered_sendsInvoice() {
        when(userRepository.findByUuidAndIsDeletedFalse("buyer-1")).thenReturn(Optional.of(sampleUser));
        when(orderServiceClient.getInvoice(eq("order-1"), any()))
                .thenReturn("PDFBYTES".getBytes());

        consumer.handleOrderStatusChanged(deliveredEvent);

        verify(notificationService).sendNotification(eq("buyer-1"), eq(NotificationType.ORDER_DELIVERED), anyString(), anyString(), eq("order-1"));
        verify(emailService).sendHtmlEmail(eq("foo@bar.com"), anyString(), anyString());
        verify(emailService).sendInvoiceEmail(eq("foo@bar.com"), eq("order-1"), anyString());
    }
}
