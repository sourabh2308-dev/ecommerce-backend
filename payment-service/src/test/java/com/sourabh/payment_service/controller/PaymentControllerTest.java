package com.sourabh.payment_service.controller;

import com.sourabh.payment_service.gateway.PaymentGateway;
import com.sourabh.payment_service.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    @DisplayName("gatewayWebhook: missing required fields returns 400")
    void gatewayWebhook_missingFields_returnsBadRequest() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("razorpay_order_id", "order-1");
        payload.put("event", "payment.captured");

        var response = paymentController.gatewayWebhook(payload, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentGateway, paymentService);
    }

    @Test
    @DisplayName("gatewayWebhook: invalid signature returns 401")
    void gatewayWebhook_invalidSignature_returnsUnauthorized() {
        Map<String, Object> payload = validPayload("payment.captured");
        when(paymentGateway.verify("order-1", "pay-1", "sig-1")).thenReturn(false);

        var response = paymentController.gatewayWebhook(payload, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentGateway).verify("order-1", "pay-1", "sig-1");
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("gatewayWebhook: valid captured event marks callback as success")
    void gatewayWebhook_capturedEvent_successTrue() {
        Map<String, Object> payload = validPayload("payment.captured");
        when(paymentGateway.verify("order-1", "pay-1", "sig-1")).thenReturn(true);

        var response = paymentController.gatewayWebhook(payload, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleGatewayCallback("order-1", true, "pay-1");
    }

    @Test
    @DisplayName("gatewayWebhook: failed event marks callback as failure")
    void gatewayWebhook_failedEvent_successFalse() {
        Map<String, Object> payload = validPayload("payment.failed");
        when(paymentGateway.verify("order-1", "pay-1", "sig-1")).thenReturn(true);

        var response = paymentController.gatewayWebhook(payload, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleGatewayCallback("order-1", false, "pay-1");
    }

    @Test
    @DisplayName("gatewayWebhook: header signature takes precedence over payload signature")
    void gatewayWebhook_headerSignature_precedence() {
        Map<String, Object> payload = validPayload("payment.captured");
        when(paymentGateway.verify("order-1", "pay-1", "header-sig")).thenReturn(true);

        var response = paymentController.gatewayWebhook(payload, "header-sig");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentGateway).verify("order-1", "pay-1", "header-sig");
        verify(paymentService).handleGatewayCallback("order-1", true, "pay-1");
    }

    private Map<String, Object> validPayload(String eventName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("razorpay_order_id", "order-1");
        payload.put("razorpay_payment_id", "pay-1");
        payload.put("razorpay_signature", "sig-1");
        payload.put("event", eventName);
        return payload;
    }
}
