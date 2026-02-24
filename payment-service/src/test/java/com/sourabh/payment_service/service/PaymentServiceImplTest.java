package com.sourabh.payment_service.service;

import com.sourabh.payment_service.dto.PaymentRequest;
import com.sourabh.payment_service.entity.Payment;
import com.sourabh.payment_service.entity.PaymentStatus;
import com.sourabh.payment_service.exception.PaymentAccessException;
import com.sourabh.payment_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.service.impl.PaymentServiceImpl;
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

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PaymentRequest();
        validRequest.setOrderUuid("order-uuid");
        validRequest.setAmount(250.00);
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
        validRequest.setBuyerUuid("spoofed-uuid");   // attacker tries to inject different UUID
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.initiatePayment(validRequest, "BUYER", "real-buyer-uuid");

        // The overwritten buyerUuid should be the header value, not the body value
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
}
