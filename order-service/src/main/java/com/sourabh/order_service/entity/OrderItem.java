package com.sourabh.order_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing a single line item within an {@link Order}.
 *
 * <p>Each {@code OrderItem} captures a snapshot of the product at the time of
 * purchase — including its name, category, image URL, seller, unit price,
 * and ordered quantity. This snapshot ensures that subsequent changes to the
 * product catalogue do not alter historical order data.</p>
 *
 * <p>Mapped to the {@code order_item} table with a foreign-key relationship
 * to the {@code orders} table via the {@code order_id} column.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Order
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    /**
     * Database-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID of the product being ordered, referencing the product-service catalogue.
     */
    private String productUuid;

    /**
     * Display name of the product at the time the order was placed.
     */
    private String productName;

    /**
     * Category of the product (e.g. "Electronics", "Clothing").
     */
    private String productCategory;

    /**
     * URL of the product's primary image. Stored with a maximum length of
     * 1000 characters to accommodate long CDN URLs.
     */
    @Column(length = 1000)
    private String productImageUrl;

    /**
     * UUID of the seller who owns the product. Used for multi-seller order
     * splitting and seller-specific dashboards.
     */
    private String sellerUuid;

    /**
     * Unit price of the product at the time of purchase, in the order's currency.
     */
    private Double price;

    /**
     * Number of units of this product ordered by the buyer.
     */
    private Integer quantity;

    /**
     * The parent {@link Order} this item belongs to. The owning side of the
     * {@code Order ↔ OrderItem} one-to-many relationship; the foreign key
     * column is {@code order_id}.
     */
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
