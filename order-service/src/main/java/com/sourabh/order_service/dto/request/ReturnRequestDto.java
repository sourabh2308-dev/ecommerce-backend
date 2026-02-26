package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for initiating a return or exchange on an order.
 */
@Data
public class ReturnRequestDto {

    @NotBlank
    private String orderUuid;

    @NotBlank
    private String returnType; // REFUND or EXCHANGE

    @NotBlank
    private String reason;
}
