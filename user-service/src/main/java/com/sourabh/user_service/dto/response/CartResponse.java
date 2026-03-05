package com.sourabh.user_service.dto.response;

import lombok.*;

import java.util.List;

/**
 * Response payload summarising the user's shopping cart.
 *
 * <p>Aggregates individual {@link CartItemResponse} entries together with
 * computed totals for display in the cart page / checkout flow.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    /** List of items currently in the cart. */
    private List<CartItemResponse> items;

    /** Total number of individual product units across all items. */
    private int totalItems;

    /** Combined monetary value of all items in the cart. */
    private double totalAmount;
}
