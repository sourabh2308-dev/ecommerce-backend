package com.sourabh.payment_service.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class RazorpayGateway implements PaymentGateway {

    private final String keyId;

    private final String keySecret;

    private final RestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public RazorpayGateway(
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret,
            RestTemplate rest) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("razorpay.key-id must be configured");
        }
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.rest = rest != null ? rest : new RestTemplate();
    }

    RazorpayGateway(String keyId, String keySecret) {
        this(keyId, keySecret, new RestTemplate());
    }

    @Override
    public String initiate(double amount, String currency, String receipt) {
        Map<String, Object> req = new HashMap<>();
        req.put("amount", (int) Math.round(amount * 100));
        req.put("currency", currency);
        req.put("receipt", receipt);

        String url = "https://api.razorpay.com/v1/orders";
        String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
        httpHeaders.set("Authorization", "Basic " + auth);
        httpHeaders.set("Content-Type", "application/json");
        org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(req, httpHeaders);
        try {
            String response = rest.postForObject(url, entity, String.class);
            JsonNode node = mapper.readTree(response);
            return node.get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error creating Razorpay order", e);
        }
    }

    @Override
    public boolean verify(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        String expected = HmacUtils.hmacSha256Hex(keySecret, payload);
        return expected.equals(signature);
    }
}
