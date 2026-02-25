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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ========================
    // Business Information
    // ========================

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessType; // e.g. INDIVIDUAL, COMPANY, PARTNERSHIP

    @Column(length = 20)
    private String gstNumber;

    @Column(length = 15)
    private String panNumber;

    // ========================
    // Address
    // ========================

    @Column(nullable = false)
    private String addressLine1;

    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    // ========================
    // ID Verification
    // ========================

    @Column(nullable = false)
    private String idType; // e.g. AADHAAR, PASSPORT, DRIVING_LICENSE

    @Column(nullable = false)
    private String idNumber;

    // ========================
    // Bank Details
    // ========================

    @Column(nullable = false)
    private String bankAccountNumber;

    @Column(nullable = false, length = 20)
    private String bankIfscCode;

    @Column(nullable = false)
    private String bankName;

    // ========================
    // Timestamps
    // ========================

    private LocalDateTime submittedAt;

    private LocalDateTime verifiedAt;
}
