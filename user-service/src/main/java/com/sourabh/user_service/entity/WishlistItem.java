package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a product that a {@link User} has saved
 * to their wishlist for future consideration.
 * <p>
 * A unique constraint on {@code (user_id, product_uuid)} ensures that
 * each product appears at most once per user's wishlist. Product
 * metadata (name, image, price) is de-normalised at the time of
 * addition.
 * </p>
 *
 * <p>Mapped to the {@code wishlist_items} table.</p>
 *
 * @see User
 */
@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_uuid"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns this wishlist entry (many-to-one, lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** UUID of the product in the product-service catalogue. */
    @Column(nullable = false)
    private String productUuid;

    /** Snapshot of the product name at the time it was wishlisted. */
    private String productName;

    /** URL or path to the product's thumbnail image. */
    private String productImage;

    /** Product price at the time the item was added. */
    @Column(nullable = false)
    private double price;

    /** Timestamp of when the item was added to the wishlist. */
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback that sets {@link #createdAt} to the
     * current time when the entity is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
