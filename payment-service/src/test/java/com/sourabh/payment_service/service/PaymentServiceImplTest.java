package com.sourabh.payment_service.service;

import com.sourabh.payment_service.dto.PaymentRequest;
import com.sourabh.payment_service.entity.Payment;
import com.sourabh.payment_service.entity.PaymentStatus;
import com.sourabh.payment_service.exception.PaymentAccessException;
import com.sourabh.payment_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.service.impl.PaymentServiceImpl;
import com.sourabh.payment_service.dto.PaymentResponse;
import com.sourabh.payment_service.exception.PaymentNotFoundException;
import com.sourabh.payment_service.repository.PaymentSplitRepository;
import com.sourabh.payment_service.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private com.sourabh.payment_service.gateway.PaymentGateway paymentGateway;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private PaymentSplitRepository paymentSplitRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PaymentRequest();
        validRequest.setOrderUuid("order-uuid");
        validRequest.setAmount(250.00);
        lenient().when(paymentGateway.initiate(anyDouble(), anyString(), anyString()))
                .thenReturn("Payment SUCCESS");
    }

    @Test
    @DisplayName("initiatePayment: fails — role is not BUYER")
    void initiatePayment_notBuyer_throwsPaymentAccessException() {
        assertThatThrownBy(() -> paymentService.initiatePayment(validRequest, "ADMIN", "admin-uuid"))
                .isInstanceOf(PaymentAccessException.class)
                .hasMessageContaining("Only buyers");

        verifyNoInteractions(paymentRepository, kafkaTemplate);
    }

    @Test
    @DisplayName("initiatePayment: stamps buyerUuid from header, not request body")
    void initiatePayment_stampsBuyerUuid() {
        validRequest.setBuyerUuid("spoofed-uuid");   
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.initiatePayment(validRequest, "BUYER", "real-buyer-uuid");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getBuyerUuid()).isEqualTo("real-buyer-uuid");
    }

    @Test
    @DisplayName("initiatePayment: always publishes PaymentCompletedEvent to Kafka")
    void initiatePayment_alwaysPublishesKafkaEvent() {
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.initiatePayment(validRequest, "BUYER", "buyer-uuid");

        verify(kafkaTemplate).send(eq("payment.completed"), any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("initiatePayment: event contains correct orderUuid")
    void initiatePayment_kafkaEventHasCorrectOrderUuid() {
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.initiatePayment(validRequest, "BUYER", "buyer-uuid");

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(kafkaTemplate).send(eq("payment.completed"), eventCaptor.capture());

        assertThat(eventCaptor.getValue().getOrderUuid()).isEqualTo("order-uuid");
    }

    @Test
    @DisplayName("initiatePayment: saves payment twice — once INITIATED, once final status")
    void initiatePayment_savesTwice() {
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.initiatePayment(validRequest, "BUYER", "buyer-uuid");

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @RepeatedTest(10)
    @DisplayName("initiatePayment: final status is always SUCCESS or FAILED (never null)")
    void initiatePayment_finalStatusNeverNull() {
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = paymentService.initiatePayment(validRequest, "BUYER", "buyer-uuid");

        assertThat(result).isIn("Payment SUCCESS", "Payment FAILED");
    }

    @Test
    @DisplayName("initiatePayment: amount is stored correctly in Payment entity")
    void initiatePayment_amountStoredCorrectly() {
        validRequest.setAmount(999.99);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.initiatePayment(validRequest, "BUYER", "buyer-uuid");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        
        Payment savedPayment = captor.getAllValues().get(0);
        assertThat(savedPayment.getAmount()).isEqualTo(999.99);
    }

    @Test
    @DisplayName("initiatePayment: order UUID correctly passed through to Kafka event")
    void initiatePayment_orderUuidInKafkaEvent() {
        validRequest.setOrderUuid("special-order-123");
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.initiatePayment(validRequest, "BUYER", "buyer-uuid");

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(kafkaTemplate).send(eq("payment.completed"), eventCaptor.capture());

        assertThat(eventCaptor.getValue().getOrderUuid()).isEqualTo("special-order-123");
    }

    @Test
    @DisplayName("initiatePayment: uses gateway and handles pending response")
    void initiatePayment_gatewayPendingLeadsToPendingStatus() {
        when(paymentGateway.initiate(anyDouble(), anyString(), anyString()))
                .thenReturn("razorpay_order_12345");
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = paymentService.initiatePayment(validRequest, "BUYER", "buyer-uuid");
        assertThat(result).isEqualTo("razorpay_order_12345");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        Payment saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getGatewayOrderId()).isEqualTo("razorpay_order_12345");

        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(paymentGateway).initiate(eq(250.00), eq("INR"), anyString());
    }

    @Test
    @DisplayName("handleGatewayCallback: updates status and publishes event")
    void handleGatewayCallback_updatesAndPublishes() {
        Payment existing = Payment.builder()
                .uuid("pay-1")
                .orderUuid("order-123")
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByGatewayOrderId("order-123")).thenReturn(java.util.Optional.empty());
        when(paymentRepository.findByOrderUuid("order-123")).thenReturn(java.util.Optional.of(existing));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(processedEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.handleGatewayCallback("order-123", true, "pay-abc");

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(kafkaTemplate).send(eq("payment.completed"), any());
    }

    @Test
    @DisplayName("handleGatewayCallback: duplicate eventId is idempotent — no Kafka event")
    void handleGatewayCallback_duplicate_skipsProcessing() {
        when(processedEventRepository.existsByEventId(any())).thenReturn(true);

        paymentService.handleGatewayCallback("order-999", true, "pay-dup");

        verifyNoInteractions(paymentRepository, kafkaTemplate);
    }

    @Test
    @DisplayName("handleGatewayCallback: failed payment publishes FAILED event")
    void handleGatewayCallback_failed_publishesFailedEvent() {
        Payment existing = Payment.builder()
                .uuid("pay-2")
                .orderUuid("order-456")
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByGatewayOrderId("order-456")).thenReturn(java.util.Optional.of(existing));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(processedEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.handleGatewayCallback("order-456", false, "pay-xyz");

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.FAILED);
        ArgumentCaptor<PaymentCompletedEvent> captor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(kafkaTemplate).send(eq("payment.completed"), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("getPaymentByUuid: buyer can view own payment")
    void getPaymentByUuid_buyerOwn_succeeds() {
        Payment pay = Payment.builder()
                .uuid("pay-uuid")
                .orderUuid("order-001")
                .buyerUuid("buyer-uuid")
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByUuid("pay-uuid")).thenReturn(java.util.Optional.of(pay));
        when(paymentSplitRepository.findByPaymentUuid("pay-uuid")).thenReturn(java.util.List.of());

        PaymentResponse response = paymentService.getPaymentByUuid("pay-uuid", "BUYER", "buyer-uuid");

        assertThat(response.getUuid()).isEqualTo("pay-uuid");
    }

    @Test
    @DisplayName("getPaymentByUuid: buyer cannot view another buyer's payment")
    void getPaymentByUuid_buyerOther_throwsPaymentAccessException() {
        Payment pay = Payment.builder()
                .uuid("pay-uuid")
                .orderUuid("order-001")
                .buyerUuid("real-buyer")
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByUuid("pay-uuid")).thenReturn(java.util.Optional.of(pay));

        assertThatThrownBy(() -> paymentService.getPaymentByUuid("pay-uuid", "BUYER", "attacker-uuid"))
                .isInstanceOf(PaymentAccessException.class)
                .hasMessageContaining("your own payments");
    }

    @Test
    @DisplayName("getPaymentByUuid: admin can view any payment")
    void getPaymentByUuid_admin_canViewAny() {
        Payment pay = Payment.builder()
                .uuid("pay-uuid")
                .orderUuid("order-001")
                .buyerUuid("some-buyer")
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByUuid("pay-uuid")).thenReturn(java.util.Optional.of(pay));
        when(paymentSplitRepository.findByPaymentUuid("pay-uuid")).thenReturn(java.util.List.of());

        PaymentResponse response = paymentService.getPaymentByUuid("pay-uuid", "ADMIN", "admin-uuid");

        assertThat(response.getUuid()).isEqualTo("pay-uuid");
    }

    @Test
    @DisplayName("getPaymentByUuid: not found throws PaymentNotFoundException")
    void getPaymentByUuid_notFound_throwsPaymentNotFoundException() {
        when(paymentRepository.findByUuid("ghost")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByUuid("ghost", "ADMIN", "admin"))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
