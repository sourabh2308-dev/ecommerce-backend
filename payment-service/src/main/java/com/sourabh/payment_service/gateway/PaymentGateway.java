package com.sourabh.payment_service.gateway;

/**
 * Strategy interface abstracting the external payment provider.
 *
 * <p>The application can switch between implementations without changing
 * service-layer logic:
 * <ul>
 *   <li>{@link MockPaymentGateway} — returns randomised success/failure
 *       strings; used for local development and automated tests.</li>
 *   <li>{@link RazorpayGateway} — calls the Razorpay Orders API over
 *       HTTPS and returns the gateway order ID; supports HMAC-SHA256
 *       webhook signature verification.</li>
 * </ul>
 *
 * <p>The active implementation is selected at startup by
 * {@link com.sourabh.payment_service.config.PaymentGatewayConfig}.
 *
 * @see com.sourabh.payment_service.config.PaymentGatewayConfig
 */
public interface PaymentGateway {

    /**
     * Initiates a payment of the specified amount with the external provider.
     *
     * <p>The return value is provider-specific.  The service layer interprets
     * strings starting with {@code "Payment SUCCESS"} or
     * {@code "Payment FAILED"} as terminal outcomes; any other value is
     * treated as an external order ID and the payment remains in
     * {@code PENDING} status until a webhook callback arrives.
     *
     * @param amount   monetary value in the smallest unit expected by the
     *                 gateway (INR for this platform)
     * @param currency ISO 4217 currency code (always {@code "INR"} for now)
     * @param receipt  unique receipt identifier (the internal payment UUID)
     * @return provider-specific result string
     */
    String initiate(double amount, String currency, String receipt);

    /**
     * Verifies the signature accompanying an asynchronous gateway callback.
     *
     * <p>The default implementation returns {@code true}, which is appropriate
     * for gateways that do not support webhook signatures (e.g. mock).
     *
     * @param orderId   gateway order identifier
     * @param paymentId gateway payment identifier
     * @param signature signature value from the webhook header or body
     * @return {@code true} if the signature is valid
     */
    default boolean verify(String orderId, String paymentId, String signature) {
        return true;
    }
}
