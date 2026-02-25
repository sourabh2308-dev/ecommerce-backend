package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OTPVerification extends BaseAuditEntity {

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


    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private String otpCode;

    @Enumerated(EnumType.STRING)
    // @Enumerated applied to field below
    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private OTPType type;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private LocalDateTime expiryTime;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private boolean verified;

    @Column(nullable = false)
    // Database column mapping
    // @Column - JPA persistence configuration
    // Database column mapping
    // @Column - JPA persistence configuration
    private int attemptCount;

    private LocalDateTime lastSentAt;


    // Relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    // @ManyToOne applied to field below
    // @ManyToOne applied to field below
    @JoinColumn(name = "user_id", nullable = false)
    // @JoinColumn applied to field below
    // @JoinColumn applied to field below
    private User user;

    // Lifecycle
    @PrePersist
    /**
     * ONCREATEOTP - Method Documentation
     *
     * PURPOSE:
     * This method handles the onCreateOTP operation.
     *
     * ANNOTATIONS USED:
     * @ManyToOne - Applied to this method
     * @JoinColumn - Applied to this method
     * @PrePersist - Applied to this method
     *
     */
    protected void onCreateOTP() {
        this.verified = false;
        this.attemptCount = 0;
        this.lastSentAt = LocalDateTime.now();

    }
}
