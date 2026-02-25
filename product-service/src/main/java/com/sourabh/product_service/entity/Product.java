package com.sourabh.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseAuditEntity {

    @Id
    // Database column mapping
    // @Id - JPA persistence configuration
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    private Long id;

    /**


     * DATABASE COLUMN MAPPING


     * 


     * @Column configures how this field maps to database column:


     * - name: Actual column name in table (default: field name in snake_case)


     * - nullable: Can be NULL in database (default: true)


     * - unique: Enforces uniqueness constraint (default: false)


     * - length: Max length for VARCHAR columns (default: 255)


     * - updatable: Can be modified after insert (default: true)


     * - insertable: Included in INSERT statements (default: true)


     * 


     * JPA auto-generates SQL schema based on these annotations.


     */


    @Column(unique = true, nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String uuid;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String name;

    @Column(length = 2000)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String description;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private Double price;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private Integer stock;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String category;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String sellerUuid;

    @Enumerated(EnumType.STRING)
    // @Enumerated applied to field below
    // @Enumerated applied to field below
    private ProductStatus status;

    @Builder.Default
    // @Builder applied to field below
    // @Builder applied to field below
    private Boolean isDeleted = false;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    @Builder.Default
    // @Builder applied to field below
    // @Builder applied to field below
    private Double averageRating = 0.0;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    @Builder.Default
    // @Builder applied to field below
    // @Builder applied to field below
    private Integer totalReviews = 0;

    @Column(columnDefinition = "TEXT")
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String imageUrl;

}
