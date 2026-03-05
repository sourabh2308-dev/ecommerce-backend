package com.sourabh.user_service.service;

import com.sourabh.user_service.dto.request.WishlistRequest;
import com.sourabh.user_service.dto.response.WishlistItemResponse;

import java.util.List;

/**
 * Service interface for user wishlist management.
 *
 * <p>Allows users to save products they are interested in for later purchase.
 * Duplicate product entries are prevented; attempting to add an already-present
 * product throws a {@link com.sourabh.user_service.exception.UserStateException}.</p>
 *
 * @see com.sourabh.user_service.service.impl.WishlistServiceImpl
 */
public interface WishlistService {

    /**
     * Retrieves all wishlist items for the specified user, ordered by creation
     * date descending (most recently added first).
     *
     * @param userUuid the UUID of the authenticated user
     * @return list of {@link WishlistItemResponse} DTOs
     */
    List<WishlistItemResponse> getWishlist(String userUuid);

    /**
     * Adds a product to the user's wishlist.
     *
     * @param userUuid the UUID of the authenticated user
     * @param request  product UUID, name, image URL, and price
     * @return the updated full wishlist
     * @throws com.sourabh.user_service.exception.UserStateException if the product is already wishlisted
     */
    List<WishlistItemResponse> addToWishlist(String userUuid, WishlistRequest request);

    /**
     * Removes a product from the user's wishlist by product UUID.
     *
     * @param userUuid    the UUID of the authenticated user
     * @param productUuid the UUID of the product to remove
     * @return the updated full wishlist
     */
    List<WishlistItemResponse> removeFromWishlist(String userUuid, String productUuid);

    /**
     * Checks whether a specific product is in the user's wishlist.
     *
     * @param userUuid    the UUID of the authenticated user
     * @param productUuid the UUID of the product to check
     * @return {@code true} if the product is in the wishlist, {@code false} otherwise
     */
    boolean isInWishlist(String userUuid, String productUuid);
}
