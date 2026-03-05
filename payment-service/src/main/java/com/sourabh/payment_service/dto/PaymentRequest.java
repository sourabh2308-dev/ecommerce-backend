package com.sourabh.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Inbound DTO for payment initiation requests ({@code POST /api/payment}).
 *
 * <p>Validated automatically by Spring's {@code @Valid} annotation on the
 * controller parameter.  The {@code buyerUuid} field is overwritten by the
 * controller with the value from the gateway-injected {@code X-User-UUID}
 * header to prevent spoofing.
 */
@Getter
@Setter
public class PaymentRequest {

    /** UUID of the order being paid for. */
    @NotBlank(message = "Order UUID is required")
    private String orderUuid;

    /** Total payment amount in the platform currency (INR). */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    /** UUID of the buyer initiating the payment (overwritten server-side). */
    @NotBlank(message = "Buyer UUID is required")
    private String buyerUuid;
}
