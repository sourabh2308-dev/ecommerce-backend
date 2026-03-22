package com.sourabh.payment_service.gateway;

public interface PaymentGateway {

    String initiate(double amount, String currency, String receipt);

    default boolean verify(String orderId, String paymentId, String signature) {
        return true;
    }
}
