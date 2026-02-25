package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PaymentResponse {

    private String uuid;
    private String orderUuid;
    private String buyerUuid;
    private Double amount;
    private String status;
    private List<PaymentSplitResponse> splits;
    private LocalDateTime createdAt;
}
