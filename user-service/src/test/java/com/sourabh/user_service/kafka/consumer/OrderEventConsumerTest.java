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

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer Tests")
class OrderEventConsumerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private OrderEventConsumer consumer;

    private OrderStatusChangedEvent deliveredEvent;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        deliveredEvent = new OrderStatusChangedEvent("order-1", "buyer-1", "SHIPPED", "DELIVERED", 100.0, "INR");
        sampleUser = new User();
        sampleUser.setUuid("buyer-1");
        sampleUser.setEmail("foo@bar.com");
        sampleUser.setFirstName("Foo");
        // internalSecret is normally injected by Spring; tests run without context so it will be null.
        // either make stub lenient (see below) or explicitly provide a value so that the
        // consumer calls orderServiceClient.getInvoice with a non-null second arg.
        org.springframework.test.util.ReflectionTestUtils.setField(consumer, "internalSecret", "test-secret");
    }

    @Test
    @DisplayName("handleOrderStatusChanged should send invoice when delivered")
    void handleDelivered_sendsInvoice() {
        when(userRepository.findByUuidAndIsDeletedFalse("buyer-1")).thenReturn(Optional.of(sampleUser));
        // allow null or any string for the secret so strict stubbing does not fail
        when(orderServiceClient.getInvoice(eq("order-1"), any()))
                .thenReturn("PDFBYTES".getBytes());

        consumer.handleOrderStatusChanged(deliveredEvent);

        verify(notificationService).sendNotification(eq("buyer-1"), eq(NotificationType.ORDER_DELIVERED), anyString(), anyString(), eq("order-1"));
        verify(emailService).sendHtmlEmail(eq("foo@bar.com"), anyString(), anyString());
        verify(emailService).sendInvoiceEmail(eq("foo@bar.com"), eq("order-1"), anyString());
    }
}
