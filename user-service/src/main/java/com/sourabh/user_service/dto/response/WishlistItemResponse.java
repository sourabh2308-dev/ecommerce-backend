package com.sourabh.user_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response payload representing a single item on the user's wishlist.
 *
 * <p>Includes denormalised product data so that the wishlist can be
 * rendered without additional calls to the product-service.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {

    /** Internal database identifier of the wishlist entry. */
    private Long id;

    /** UUID of the wishlisted product. */
    private String productUuid;

    /** Denormalised product display name. */
    private String productName;

    /** Denormalised product thumbnail URL. */
    private String productImage;

    /** Price of the product at the time it was wishlisted. */
    private double price;

    /** Timestamp when the item was added to the wishlist. */
    private LocalDateTime createdAt;
}
