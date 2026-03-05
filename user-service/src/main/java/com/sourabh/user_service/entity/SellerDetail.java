package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity storing business, identity-verification, and banking
 * information for a seller on the platform.
 * <p>
 * There is a strict one-to-one relationship with {@link User} – only
 * users whose {@link Role} is {@code SELLER} will have a corresponding
 * {@code SellerDetail} row. The admin reviews the submitted details
 * before approving the seller account.
 * </p>
 *
 * <p>Mapped to the {@code seller_details} table. Inherits audit
 * timestamps from {@link BaseAuditEntity}.</p>
 *
 * @see User
 * @see BaseAuditEntity
 */
@Entity
@Table(name = "seller_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDetail extends BaseAuditEntity {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user account associated with this seller profile (one-to-one, lazy-loaded). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Registered business / shop name. */
    @Column(nullable = false)
    private String businessName;

    /** Type of business entity, e.g. INDIVIDUAL, COMPANY, PARTNERSHIP. */
    @Column(nullable = false)
    private String businessType;

    /** Goods and Services Tax registration number (up to 20 characters). */
    @Column(length = 20)
    private String gstNumber;

    /** Permanent Account Number for tax identification (up to 15 characters). */
    @Column(length = 15)
    private String panNumber;

    /** Primary street / building address of the business. */
    @Column(nullable = false)
    private String addressLine1;

    /** Optional secondary address line. */
    private String addressLine2;

    /** City where the business is located. */
    @Column(nullable = false)
    private String city;

    /** State or province of the business address. */
    @Column(nullable = false)
    private String state;

    /** Postal / ZIP code (max 10 characters). */
    @Column(nullable = false, length = 10)
    private String pincode;

    /** Type of government-issued ID provided, e.g. AADHAAR, PASSPORT, DRIVING_LICENSE. */
    @Column(nullable = false)
    private String idType;

    /** Government-issued identification number. */
    @Column(nullable = false)
    private String idNumber;

    /** Bank account number for payout settlement. */
    @Column(nullable = false)
    private String bankAccountNumber;

    /** Indian Financial System Code of the bank branch (up to 20 characters). */
    @Column(nullable = false, length = 20)
    private String bankIfscCode;

    /** Name of the bank where the seller holds the settlement account. */
    @Column(nullable = false)
    private String bankName;

    /** Timestamp when the seller submitted their verification details. */
    private LocalDateTime submittedAt;

    /** Timestamp when an admin verified / approved the seller details. */
    private LocalDateTime verifiedAt;
}
