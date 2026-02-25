package com.sourabh.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

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


    @Column(unique = true)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String uuid;

    private String orderUuid;

    private String buyerUuid;

    private Double amount;

    @Enumerated(EnumType.STRING)
    // @Enumerated applied to field below
    // @Enumerated applied to field below
    private PaymentStatus status;

    private LocalDateTime createdAt;
}
