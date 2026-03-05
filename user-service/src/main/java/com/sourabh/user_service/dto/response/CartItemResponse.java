package com.sourabh.user_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response payload representing a single item in the user's shopping cart.
 *
 * <p>Includes denormalised product data (name, image, price) to avoid
 * additional round-trips to the product-service when rendering the cart.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    /** Internal database identifier of the cart item. */
    private Long id;

    /** UUID of the associated product. */
    private String productUuid;

    /** Denormalised product display name. */
    private String productName;

    /** Denormalised product thumbnail URL. */
    private String productImage;

    /** Unit price of the product. */
    private double price;

    /** Number of units of this product in the cart. */
    private int quantity;

    /** Computed subtotal ({@code price * quantity}). */
    private double subtotal;

    /** Timestamp when the item was added to the cart. */
    private LocalDateTime createdAt;
}
