package com.sourabh.order_service.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object representing a product as returned by the
 * product-service's internal API.
 *
 * <p>Used by the order-service when creating orders to validate stock
 * availability, fetch pricing, resolve seller information, and populate
 * order-item details.</p>
 */
@Getter
@Setter
public class ProductDto {

    /** Universally unique identifier of the product. */
    private String uuid;

    /** Display name of the product. */
    private String name;

    /** Unit price of the product in the platform's base currency. */
    private Double price;

    /** Current available stock quantity. */
    private Integer stock;

    /** UUID of the seller who listed this product. */
    private String sellerUuid;

    /** Category to which the product belongs (e.g., "Electronics", "Clothing"). */
    private String category;

    /** URL of the product's primary image. */
    private String imageUrl;

    /** Current status of the product (e.g., {@code "ACTIVE"}, {@code "INACTIVE"}). */
    private String status;
}
