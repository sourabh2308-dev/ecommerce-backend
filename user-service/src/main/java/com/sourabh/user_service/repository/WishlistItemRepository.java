package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WishlistItem} entities.
 * <p>
 * Provides queries for listing, checking, and removing wishlist
 * entries scoped to a specific {@link User}.
 * </p>
 *
 * @see WishlistItem
 */
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    /**
     * Returns all wishlist items for the given user, sorted newest-first.
     *
     * @param user the owning {@link User}
     * @return the user's wishlist items in reverse chronological order
     */
    List<WishlistItem> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds a specific wishlist entry by user and product UUID.
     *
     * @param user        the owning {@link User}
     * @param productUuid the product's public UUID
     * @return the matching wishlist item, if present
     */
    Optional<WishlistItem> findByUserAndProductUuid(User user, String productUuid);

    /**
     * Checks whether a product is already in the user's wishlist.
     *
     * @param user        the owning {@link User}
     * @param productUuid the product's public UUID
     * @return {@code true} if the product is wishlisted
     */
    boolean existsByUserAndProductUuid(User user, String productUuid);

    /**
     * Deletes a wishlist entry identified by user and product UUID.
     *
     * @param user        the owning {@link User}
     * @param productUuid the product's public UUID
     */
    void deleteByUserAndProductUuid(User user, String productUuid);
}
