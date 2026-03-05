package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for adding a product to the user's wishlist.
 *
 * <p>The {@code productUuid} is required; {@code productName} and
 * {@code productImage} are denormalised copies stored alongside the
 * wishlist entry for efficient retrieval.</p>
 */
@Getter
@Setter
public class WishlistRequest {

    /** UUID of the product to wishlist. */
    @NotBlank(message = "Product UUID is required")
    private String productUuid;

    /** Denormalised product display name. */
    private String productName;

    /** Denormalised product thumbnail URL. */
    private String productImage;

    /** Product price at the time of wishlisting. */
    @Positive(message = "Price must be positive")
    private double price;
}
