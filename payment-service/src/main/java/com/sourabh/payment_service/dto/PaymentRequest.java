package com.sourabh.payment_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {

    private String orderUuid;
    private Double amount;
    private String buyerUuid;
}
