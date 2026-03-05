package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound DTO representing a single payment and its associated revenue splits.
 *
 * <p>Built from the {@link com.sourabh.payment_service.entity.Payment} entity
 * by the service layer's mapping helper.  The embedded
 * {@link PaymentSplitResponse} list shows how the payment amount was
 * distributed across sellers, platform fees, and delivery charges.
 */
@Getter
@Builder
public class PaymentResponse {

    /** Unique identifier of the payment. */
    private String uuid;

    /** UUID of the order this payment belongs to. */
    private String orderUuid;

    /** UUID of the buyer who made the payment. */
    private String buyerUuid;

    /** Total payment amount (INR). */
    private Double amount;

    /** Current status string (e.g. {@code INITIATED}, {@code PENDING}, {@code SUCCESS}, {@code FAILED}). */
    private String status;

    /** Per-seller revenue breakdown for this payment. */
    private List<PaymentSplitResponse> splits;

    /** Timestamp when the payment record was created. */
    private LocalDateTime createdAt;
}
