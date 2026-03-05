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

/**
 * {@link PaymentGateway} implementation that communicates with the
 * <a href="https://razorpay.com/docs/api/orders/">Razorpay Orders API</a>
 * using plain HTTP calls via {@link RestTemplate}.
 *
 * <p>This lightweight approach avoids pulling in the official Razorpay SDK,
 * which may not be available in all build environments.  The class is only
 * activated when the property {@code payment.gateway=razorpay} is set.
 *
 * <p><b>Authentication:</b> HTTP Basic auth using the Razorpay key ID and
 * key secret.
 *
 * <p><b>Webhook verification:</b> Computes an HMAC-SHA256 hash of
 * {@code "<orderId>|<paymentId>"} using the key secret and compares it
 * with the signature supplied by the caller (header or body).
 *
 * @see com.sourabh.payment_service.config.PaymentGatewayConfig
 */
@Component
public class RazorpayGateway implements PaymentGateway {

    /** Razorpay API key identifier. */
    private final String keyId;

    /** Razorpay API key secret used for auth and HMAC verification. */
    private final String keySecret;

    /** HTTP client for outbound Razorpay API calls. */
    private final RestTemplate rest;

    /** Jackson mapper for parsing Razorpay JSON responses. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Primary constructor used by Spring when the Razorpay gateway is active.
     *
     * @param keyId     Razorpay key ID from {@code razorpay.key-id} property
     * @param keySecret Razorpay key secret from {@code razorpay.key-secret} property
     * @param rest      shared {@link RestTemplate} bean for HTTP calls
     * @throws IllegalStateException if {@code keyId} is blank
     */
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

    /**
     * Convenience constructor for unit tests that do not require a shared
     * {@link RestTemplate}.  Package-private to prevent Spring from
     * considering it during autowiring.
     *
     * @param keyId     Razorpay key ID
     * @param keySecret Razorpay key secret
     */
    RazorpayGateway(String keyId, String keySecret) {
        this(keyId, keySecret, new RestTemplate());
    }

    /**
     * Creates a Razorpay order via {@code POST /v1/orders} and returns the
     * gateway-assigned order ID (e.g. {@code order_xxx}).
     *
     * <p>The amount is converted to paise (smallest currency unit) before
     * sending because the Razorpay API expects integer paise values.
     *
     * @param amount   payment amount in INR
     * @param currency ISO currency code (e.g. {@code "INR"})
     * @param receipt  internal payment UUID used as the Razorpay receipt
     * @return the Razorpay order ID string
     * @throws RuntimeException if the API call fails or the response is unparseable
     */
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

    /**
     * Verifies a Razorpay webhook signature using HMAC-SHA256.
     *
     * <p>The expected signature is computed as:
     * {@code HMAC_SHA256(keySecret, "<orderId>|<paymentId>")}.
     *
     * @param orderId   the Razorpay order ID
     * @param paymentId the Razorpay payment ID
     * @param signature the signature to verify
     * @return {@code true} if the computed HMAC matches the provided signature
     */
    @Override
    public boolean verify(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        String expected = HmacUtils.hmacSha256Hex(keySecret, payload);
        return expected.equals(signature);
    }
}
