package com.sourabh.review_service.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing a single line item inside an {@link OrderDto}.
 *
 * <p>During review creation the service iterates over the order's items to
 * confirm that the product being reviewed is part of the order, and to
 * capture the seller UUID for the review record.
 *
 * @see OrderDto
 */
@Getter
@Setter
public class OrderItemDto {

    /** UUID of the product in this order line item. */
    private String productUuid;

    /** UUID of the seller who listed the product. */
    private String sellerUuid;
}
