package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO submitted by a buyer to initiate a return or exchange on a
 * previously delivered order.
 *
 * <p>All fields are mandatory. The {@code returnType} field indicates
 * whether the buyer is requesting a monetary refund or a product exchange.</p>
 */
@Data
public class ReturnRequestDto {

    /** UUID of the order to be returned (required). */
    @NotBlank
    private String orderUuid;

    /** Type of return: {@code "REFUND"} or {@code "EXCHANGE"} (required). */
    @NotBlank
    private String returnType;

    /** Buyer-supplied reason for requesting the return (required). */
    @NotBlank
    private String reason;
}
