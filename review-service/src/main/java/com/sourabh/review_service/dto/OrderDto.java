package com.sourabh.review_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO representing an order fetched from the order-service via
 * {@link com.sourabh.review_service.feign.OrderServiceClient}.
 *
 * <p>Used during review creation to verify that the buyer actually
 * purchased the product and that the order has been delivered.
 *
 * @see com.sourabh.review_service.feign.OrderServiceClient#getOrder(String)
 */
@Getter
@Setter
public class OrderDto {

    /** Unique identifier of the order. */
    private String uuid;

    /** UUID of the buyer who placed the order. */
    private String buyerUuid;

    /** Current order status (e.g.&nbsp;{@code DELIVERED}, {@code CONFIRMED}). */
    private String status;

    /** Line items contained in this order. */
    private List<OrderItemDto> items;
}

