package com.sourabh.payment_service.gateway;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * No-op {@link PaymentGateway} implementation used during local development
 * and in automated tests.
 *
 * <p>Returns a random {@code "Payment SUCCESS"} or {@code "Payment FAILED"}
 * string without making any external network calls.  The service layer
 * interprets these strings as terminal statuses and immediately transitions
 * the {@link com.sourabh.payment_service.entity.Payment} to its final state.
 *
 * <p>This component is always registered in the Spring context; the
 * {@link com.sourabh.payment_service.config.PaymentGatewayConfig} factory
 * decides at startup whether it or the {@link RazorpayGateway} is exposed
 * as the active {@code PaymentGateway} bean.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    /** Thread-safe random number generator for success/failure decision. */
    private final Random rng = new Random();

    /**
     * Simulates a payment initiation by randomly returning either
     * {@code "Payment SUCCESS"} or {@code "Payment FAILED"}.
     *
     * @param amount   the payment amount (ignored by mock)
     * @param currency the currency code (ignored by mock)
     * @param receipt  the receipt/payment UUID (ignored by mock)
     * @return {@code "Payment SUCCESS"} or {@code "Payment FAILED"}
     */
    @Override
    public String initiate(double amount, String currency, String receipt) {
        boolean success = rng.nextBoolean();
        return success ? "Payment SUCCESS" : "Payment FAILED";
    }
}
