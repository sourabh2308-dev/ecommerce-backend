package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Response DTO representing a single line item within an {@link OrderResponse}.
 *
 * <p>Contains product details (UUID, name, category, image, seller) as well
 * as the price and quantity captured at the time the order was placed.
 * Annotated with {@link Jacksonized} for immutable deserialisation support.</p>
 */
@Getter
@Builder
@Jacksonized
public class OrderItemResponse {

    /** UUID of the ordered product. */
    private String productUuid;

    /** Display name of the product at the time of purchase. */
    private String productName;

    /** Category of the product (e.g., "Electronics"). */
    private String productCategory;

    /** URL of the product's primary image. */
    private String productImageUrl;

    /** UUID of the seller who listed the product. */
    private String sellerUuid;

    /** Unit price of the product at the time of purchase. */
    private Double price;

    /** Number of units ordered. */
    private Integer quantity;
}
