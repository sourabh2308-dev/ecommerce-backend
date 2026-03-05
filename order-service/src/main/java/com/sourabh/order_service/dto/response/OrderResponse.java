package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a complete order and its associated metadata.
 *
 * <p>Includes buyer information, financial totals, order status, payment
 * status, line items, shipping address, order-splitting details (for
 * multi-seller orders), return information, and audit timestamps.</p>
 *
 * <p>Annotated with {@link Jacksonized} for immutable deserialisation via
 * Jackson combined with Lombok's {@link Builder}.</p>
 */
@Getter
@Builder
@Jacksonized
public class OrderResponse {

    /** Universally unique identifier of the order. */
    private String uuid;

    /** UUID of the buyer who placed the order. */
    private String buyerUuid;

    /** Total monetary amount of the order. */
    private Double totalAmount;

    /** Current order status (e.g., {@code "PENDING"}, {@code "CONFIRMED"}, {@code "DELIVERED"}). */
    private String status;

    /** Payment status (e.g., {@code "PAID"}, {@code "PENDING"}, {@code "REFUNDED"}). */
    private String paymentStatus;

    /** Line items included in this order. */
    private List<OrderItemResponse> items;

    /** Order type indicating whether this is a {@code "MAIN"} or {@code "SUB"} order (multi-seller splitting). */
    private String orderType;

    /** UUID of the parent order if this is a sub-order; {@code null} for main orders. */
    private String parentOrderUuid;

    /** Identifier grouping related main and sub-orders together. */
    private String orderGroupId;

    /** Full name of the shipping recipient. */
    private String shippingName;

    /** Street address for delivery. */
    private String shippingAddress;

    /** City for the shipping destination. */
    private String shippingCity;

    /** State or province for shipping. */
    private String shippingState;

    /** Postal / PIN code for shipping. */
    private String shippingPincode;

    /** Contact phone number for the shipping recipient. */
    private String shippingPhone;

    /** Return type requested by the buyer ({@code "REFUND"} or {@code "EXCHANGE"}), if any. */
    private String returnType;

    /** Reason provided by the buyer for returning the order, if applicable. */
    private String returnReason;

    /** Timestamp when the order was created. */
    private LocalDateTime createdAt;

    /** Timestamp when the order was last updated. */
    private LocalDateTime updatedAt;
}

