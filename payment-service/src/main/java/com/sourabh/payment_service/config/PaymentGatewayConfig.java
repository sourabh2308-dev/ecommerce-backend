package com.sourabh.payment_service.config;

import com.sourabh.payment_service.gateway.MockPaymentGateway;
import com.sourabh.payment_service.gateway.PaymentGateway;
import com.sourabh.payment_service.gateway.RazorpayGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PaymentGatewayConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${payment.gateway:mock}")
    private String gateway;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Bean
    public PaymentGateway paymentGateway(MockPaymentGateway mock, RestTemplate restTemplate) {
        if ("razorpay".equalsIgnoreCase(gateway)) {
            return new RazorpayGateway(razorpayKeyId, razorpayKeySecret, restTemplate);
        }
        return mock;
    }
}
