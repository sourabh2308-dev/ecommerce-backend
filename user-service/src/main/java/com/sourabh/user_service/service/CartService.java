package com.sourabh.user_service.service;

import com.sourabh.user_service.dto.request.CartItemRequest;
import com.sourabh.user_service.dto.response.CartResponse;

/**
 * Service interface for shopping-cart management.
 *
 * <p>Maintains a per-user cart backed by the {@code cart_items} table.
 * Items are identified by a product UUID; adding an already-present product
 * increments its quantity rather than creating a duplicate row.</p>
 *
 * @see com.sourabh.user_service.service.impl.CartServiceImpl
 */
public interface CartService {

    /**
     * Fetches the complete cart for a user, including item details and computed totals.
     *
     * @param userUuid the UUID of the authenticated user
     * @return a {@link CartResponse} containing all items, total count, and total amount
     */
    CartResponse getCart(String userUuid);

    /**
     * Adds a product to the cart or increments its quantity if already present.
     *
     * @param userUuid the UUID of the authenticated user
     * @param request  product UUID, name, image, price, and quantity
     * @return the updated {@link CartResponse}
     */
    CartResponse addToCart(String userUuid, CartItemRequest request);

    /**
     * Updates the quantity of an existing cart item.
     *
     * <p>If the new quantity is zero or negative the item is removed.</p>
     *
     * @param userUuid the UUID of the authenticated user
     * @param itemId   the database ID of the cart item
     * @param quantity the new desired quantity
     * @return the updated {@link CartResponse}
     * @throws com.sourabh.user_service.exception.UserStateException if the item is not found
     */
    CartResponse updateCartItem(String userUuid, Long itemId, int quantity);

    /**
     * Removes a single item from the cart.
     *
     * @param userUuid the UUID of the authenticated user
     * @param itemId   the database ID of the cart item to remove
     * @return the updated {@link CartResponse}
     * @throws com.sourabh.user_service.exception.UserStateException if the item is not found
     */
    CartResponse removeFromCart(String userUuid, Long itemId);

    /**
     * Empties the entire cart for the given user (deletes all cart items).
     *
     * @param userUuid the UUID of the authenticated user
     */
    void clearCart(String userUuid);
}
