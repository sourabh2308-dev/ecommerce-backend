package com.sourabh.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * PAYMENT REQUEST DTO
 * 
 * Data Transfer Object for payment initiation requests.
 * Contains order and payment details for processing.
 */
@Getter
@Setter
public class PaymentRequest {

    @NotBlank(message = "Order UUID is required")
    private String orderUuid;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Buyer UUID is required")
    private String buyerUuid;
}
