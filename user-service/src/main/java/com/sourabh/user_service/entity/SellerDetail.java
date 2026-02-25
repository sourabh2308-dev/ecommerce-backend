package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores seller's business and ID verification details.
 * One-to-one relationship with User (only for SELLER role).
 */
@Entity
@Table(name = "seller_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDetail extends BaseAuditEntity {

    @Id
    // Database column mapping
    // @Id - JPA persistence configuration
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    // Database column mapping
    // @GeneratedValue - JPA persistence configuration
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    // @OneToOne applied to field below
    // @OneToOne applied to field below
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    // @JoinColumn applied to field below
    // @JoinColumn applied to field below
    private User user;

    // ========================
    // Business Information
    // ========================

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


    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String businessName;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String businessType; // e.g. INDIVIDUAL, COMPANY, PARTNERSHIP

    @Column(length = 20)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String gstNumber;

    @Column(length = 15)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String panNumber;

    // ========================
    // Address
    // ========================

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String addressLine1;

    private String addressLine2;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String city;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String state;

    @Column(nullable = false, length = 10)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String pincode;

    // ========================
    // ID Verification
    // ========================

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String idType; // e.g. AADHAAR, PASSPORT, DRIVING_LICENSE

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String idNumber;

    // ========================
    // Bank Details
    // ========================

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String bankAccountNumber;

    @Column(nullable = false, length = 20)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String bankIfscCode;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String bankName;

    // ========================
    // Timestamps
    // ========================

    private LocalDateTime submittedAt;

    private LocalDateTime verifiedAt;
}
