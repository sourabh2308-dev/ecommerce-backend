package com.sourabh.payment_service.gateway;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Simple gateway implementation used for local development and in tests.  It
 * mimics the legacy behaviour by randomly returning either a success or
 * failure string.  When the returned string contains "SUCCESS" or "FAILED",
 * the service layer interprets that as a final status; otherwise it treats the
 * string as an external order id and leaves the payment in PENDING state.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final Random rng = new Random();

    @Override
    public String initiate(double amount, String currency, String receipt) {
        // mimic previous hard‑coded success logic while leaving room for
        // failures in the random path we used earlier in development
        boolean success = rng.nextBoolean();
        return success ? "Payment SUCCESS" : "Payment FAILED";
    }
}
