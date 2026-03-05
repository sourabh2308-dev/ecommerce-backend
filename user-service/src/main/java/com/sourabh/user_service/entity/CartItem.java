package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing a single item in a user's shopping cart.
 * <p>
 * A unique constraint on {@code (user_id, product_uuid)} prevents a user
 * from having duplicate entries for the same product; additional units
 * are represented by incrementing {@link #quantity} instead.
 * </p>
 *
 * <p>Product metadata (name, image, price) is de-normalised from the
 * product-service at the time the item is added so the cart can be
 * rendered without cross-service calls.</p>
 *
 * <p>Mapped to the {@code cart_items} table. Inherits audit timestamps
 * from {@link BaseAuditEntity}.</p>
 *
 * @see User
 * @see BaseAuditEntity
 */
@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_uuid"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseAuditEntity {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns this cart item (many-to-one, lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** UUID of the product in the product-service catalogue. */
    @Column(nullable = false)
    private String productUuid;

    /** Snapshot of the product name at the time it was added to the cart. */
    private String productName;

    /** URL or path to the product's thumbnail image. */
    private String productImage;

    /** Unit price of the product at the time it was added. */
    @Column(nullable = false)
    private double price;

    /** Number of units of this product in the cart (defaults to 1). */
    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;
}
