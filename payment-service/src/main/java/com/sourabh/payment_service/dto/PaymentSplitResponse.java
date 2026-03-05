package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Outbound DTO exposing the revenue-split details for a single order item
 * within a payment.
 *
 * <p>Each field mirrors the corresponding column in the
 * {@link com.sourabh.payment_service.entity.PaymentSplit} entity.
 * The split shows how the item amount is divided into platform fee,
 * delivery fee, and the net seller payout.
 */
@Getter
@Builder
public class PaymentSplitResponse {

    /** UUID of the seller who owns the product. */
    private String sellerUuid;

    /** UUID of the product in this line item. */
    private String productUuid;

    /** Gross item amount (price x quantity). */
    private Double itemAmount;

    /** Platform commission percentage applied (e.g. {@code 10.0} for 10 %). */
    private Double platformFeePercent;

    /** Absolute platform commission deducted from the item amount. */
    private Double platformFee;

    /** Delivery fee charged for this line item. */
    private Double deliveryFee;

    /** Net amount payable to the seller after deductions. */
    private Double sellerPayout;

    /** Split status (e.g. {@code PENDING}, {@code COMPLETED}). */
    private String status;
}
