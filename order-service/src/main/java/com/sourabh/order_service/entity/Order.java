package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order extends BaseAuditEntity {

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
    private String buyerUuid;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    // @Enumerated applied to field below
    // @Enumerated applied to field below
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    // @Enumerated applied to field below
    // @Enumerated applied to field below
    private PaymentStatus paymentStatus;

    @Builder.Default
    // @Builder applied to field below
    // @Builder applied to field below
    private Boolean isDeleted = false;

    // ── Shipping address ────────────────────────────────
    private String shippingName;
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String shippingPhone;

    /**


     * RELATIONSHIP MAPPING


     * 


     * Defines association between entities (foreign key relationship).


     * - @OneToMany: One instance has many related instances (1:N)


     * - @ManyToOne: Many instances reference one instance (N:1)


     * - @ManyToMany: Many-to-many relationship (N:M, join table)


     * - @OneToOne: One-to-one relationship (1:1, shared primary key)


     * 


     * mappedBy: Declares non-owning side (no foreign key column here)


     * cascade: Propagates operations (PERSIST, MERGE, REMOVE, etc.)


     * fetch: LAZY (load on access) or EAGER (load immediately)


     * 


     * Best practice: Use LAZY fetching to avoid N+1 query problem.


     */


    @OneToMany(mappedBy = "order",
    // @OneToMany applied to field below
    // @OneToMany applied to field below
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrderItem> items;
}

