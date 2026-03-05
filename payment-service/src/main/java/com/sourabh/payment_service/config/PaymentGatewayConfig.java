package com.sourabh.payment_service.config;

import com.sourabh.payment_service.gateway.MockPaymentGateway;
import com.sourabh.payment_service.gateway.PaymentGateway;
import com.sourabh.payment_service.gateway.RazorpayGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Factory configuration that selects the active {@link PaymentGateway}
 * implementation at startup based on the {@code payment.gateway} property.
 *
 * <p>Supported values:
 * <ul>
 *   <li><b>mock</b> (default) — uses {@link MockPaymentGateway}, which
 *       randomly returns success/failure strings without making external
 *       network calls.  Ideal for local development and automated tests.</li>
 *   <li><b>razorpay</b> — uses {@link RazorpayGateway}, which calls the
 *       Razorpay Orders API over HTTPS to create real payment orders.
 *       Requires valid {@code razorpay.key-id} and {@code razorpay.key-secret}
 *       properties.</li>
 * </ul>
 *
 * <p>A shared {@link RestTemplate} bean is also defined here so that it can
 * be injected into the Razorpay gateway (and easily mocked in tests).
 */
@Configuration
public class PaymentGatewayConfig {

    /**
     * Provides a reusable {@link RestTemplate} for outbound HTTP calls.
     *
     * @return a default {@code RestTemplate} instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /** Gateway selector property — defaults to {@code "mock"}. */
    @Value("${payment.gateway:mock}")
    private String gateway;

    /** Razorpay API key identifier (empty when mock gateway is active). */
    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    /** Razorpay API key secret (empty when mock gateway is active). */
    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    /**
     * Instantiates the appropriate {@link PaymentGateway} bean based on the
     * {@code payment.gateway} property value.
     *
     * @param mock         the mock gateway (always component-scanned)
     * @param restTemplate shared HTTP client for Razorpay calls
     * @return the selected gateway implementation
     */
    @Bean
    public PaymentGateway paymentGateway(MockPaymentGateway mock, RestTemplate restTemplate) {
        if ("razorpay".equalsIgnoreCase(gateway)) {
            return new RazorpayGateway(razorpayKeyId, razorpayKeySecret, restTemplate);
        }
        return mock;
    }
}
