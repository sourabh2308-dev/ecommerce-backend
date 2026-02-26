package com.sourabh.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A purchasable variant of a product (e.g. size=L, color=Red).
 * Each variant can override price and maintains its own stock.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_variant",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "variant_name", "variant_value"}))
public class ProductVariant extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** e.g. "Size", "Color", "Material" */
    @Column(nullable = false)
    private String variantName;

    /** e.g. "XL", "Red", "Cotton" */
    @Column(nullable = false)
    private String variantValue;

    /** Price override (null = use product base price) */
    private Double priceOverride;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(nullable = false, length = 20)
    private String sku;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    private void generateUuid() {
        if (uuid == null) uuid = java.util.UUID.randomUUID().toString();
    }
}
