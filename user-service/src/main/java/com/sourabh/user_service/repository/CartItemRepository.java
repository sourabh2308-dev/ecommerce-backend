package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.CartItem;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CartItem} entities.
 * <p>
 * Provides standard CRUD operations plus user-scoped queries for
 * cart retrieval, product look-up, and bulk deletion.
 * </p>
 *
 * @see CartItem
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Returns all cart items for the given user, newest first.
     *
     * @param user the owning {@link User}
     * @return ordered list of cart items
     */
    List<CartItem> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds a cart item by the combination of user and product UUID.
     *
     * @param user        the owning {@link User}
     * @param productUuid UUID of the product
     * @return the matching cart item, if any
     */
    Optional<CartItem> findByUserAndProductUuid(User user, String productUuid);

    /**
     * Finds a cart item by its database ID scoped to the given user.
     *
     * @param id   cart-item database ID
     * @param user the owning {@link User}
     * @return the matching cart item, if any
     */
    Optional<CartItem> findByIdAndUser(Long id, User user);

    /**
     * Deletes every cart item belonging to the given user (clear cart).
     *
     * @param user the owning {@link User}
     */
    void deleteAllByUser(User user);

    /**
     * Returns the number of distinct items in the user's cart.
     *
     * @param user the owning {@link User}
     * @return item count
     */
    int countByUser(User user);
}
