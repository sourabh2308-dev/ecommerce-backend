package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private String uuid;
    private String orderUuid;
    private String buyerUuid;
    private Double amount;
    private String status;
    private LocalDateTime createdAt;
}
