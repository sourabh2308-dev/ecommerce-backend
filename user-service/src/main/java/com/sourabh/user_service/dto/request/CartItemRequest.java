package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for adding a product to the shopping cart or updating
 * an existing cart item's quantity.
 *
 * <p>The {@code productUuid} is mandatory; other fields such as
 * {@code productName} and {@code productImage} are denormalised copies
 * stored alongside the cart entry for fast retrieval without calling the
 * product-service.</p>
 */
@Getter
@Setter
public class CartItemRequest {

    /** UUID of the product to add to the cart. */
    @NotBlank(message = "Product UUID is required")
    private String productUuid;

    /** Denormalised product display name. */
    private String productName;

    /** Denormalised product thumbnail URL. */
    private String productImage;

    /** Unit price of the product at the time of adding to cart. */
    @Positive(message = "Price must be positive")
    private double price;

    /** Desired quantity; defaults to 1. */
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
