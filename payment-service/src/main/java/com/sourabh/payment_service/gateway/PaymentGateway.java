package com.sourabh.payment_service.gateway;

import com.sourabh.payment_service.entity.PaymentStatus;

/**
 * Abstraction over an external payment provider.  The real system can switch
 * between a mock implementation (used for local development & tests) and a
 * concrete gateway such as Razorpay without changing the service logic.
 *
 * Implementations should be lightweight and stateless; any network clients
 * may be created in constructors.  The interface is intentionally simple –
 * real-world gateways typically require additional steps (webhooks, tokens)
 * which are handled by the service layer.
 */
public interface PaymentGateway {

    /**
     * Initiate a payment of the specified amount, returning a provider-specific
     * response string.  The caller is responsible for interpreting the string
     * (e.g. order id, "SUCCESS"/"FAILED" message).
     *
     * @param amount   monetary value in the gateway's expected currency units
     * @param currency ISO currency code such as "INR" (always INR for now)
     * @param receipt  unique identifier provided by the caller (usually the
     *                 internal payment UUID)
     * @return provider result string
     */
    String initiate(double amount, String currency, String receipt);

    /**
     * Perform signature verification for asynchronous callbacks/webhooks.
     * Gateways that do not support callbacks may simply return {@code true}.
     *
     * @param orderId    provider order identifier
     * @param paymentId  provider payment identifier
     * @param signature  signature header from the webhook request
     * @return {@code true} when the signature is valid
     */
    default boolean verify(String orderId, String paymentId, String signature) {
        return true;
    }
}
