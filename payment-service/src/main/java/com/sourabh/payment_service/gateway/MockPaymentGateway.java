package com.sourabh.payment_service.gateway;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MockPaymentGateway implements PaymentGateway {

    private final Random rng = new Random();

    @Override
    public String initiate(double amount, String currency, String receipt) {
        boolean success = rng.nextBoolean();
        return success ? "Payment SUCCESS" : "Payment FAILED";
    }
}
