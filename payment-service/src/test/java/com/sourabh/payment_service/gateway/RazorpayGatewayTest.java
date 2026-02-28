package com.sourabh.payment_service.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the lightweight RazorpayGateway implementation.  The RestTemplate
 * is mocked to verify that proper basic authentication header is set and that
 * the returned order id is parsed correctly.
 */
class RazorpayGatewayTest {

    @Mock
    private RestTemplate rest;

    private RazorpayGateway gateway; // constructed manually in setUp

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // use dummy credentials and inject the mocked RestTemplate
        gateway = new RazorpayGateway("rzp_test_id", "rzp_test_secret", rest);
    }

    @Test
    @DisplayName("initiate: includes authorization header and returns ID")
    void initiate_sendsAuthHeaderAndParsesId() {
        String fakeResponse = "{\"id\":\"rzp_order_123\"}";
        when(rest.postForObject(eq("https://api.razorpay.com/v1/orders"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(fakeResponse);

        String orderId = gateway.initiate(100.0, "INR", "receipt-1");

        assertThat(orderId).isEqualTo("rzp_order_123");
        // capture the HttpEntity argument to inspect headers
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(rest).postForObject(eq("https://api.razorpay.com/v1/orders"), captor.capture(), eq(String.class));
        HttpHeaders headers = captor.getValue().getHeaders();
        assertThat(headers.getFirst("Authorization")).startsWith("Basic ");
    }
}
