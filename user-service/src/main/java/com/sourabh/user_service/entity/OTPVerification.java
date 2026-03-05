package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity that records a one-time password (OTP) issued to a
 * {@link User} for verification purposes.
 * <p>
 * Each OTP has a limited lifetime ({@link #expiryTime}) and a
 * maximum number of verification attempts ({@link #attemptCount}).
 * Once successfully verified the {@link #verified} flag is set to
 * {@code true}.
 * </p>
 *
 * <p>Mapped to the {@code otp_verifications} table. Inherits audit
 * timestamps from {@link BaseAuditEntity}.</p>
 *
 * @see OTPType
 * @see User
 */
@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OTPVerification extends BaseAuditEntity {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The generated OTP code (e.g. a 6-digit numeric string). */
    @Column(nullable = false)
    private String otpCode;

    /** Purpose for which this OTP was issued. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OTPType type;

    /** Point in time after which this OTP is no longer valid. */
    @Column(nullable = false)
    private LocalDateTime expiryTime;

    /** {@code true} once the user has successfully verified this OTP. */
    @Column(nullable = false)
    private boolean verified;

    /** Running count of failed verification attempts for this OTP. */
    @Column(nullable = false)
    private int attemptCount;

    /** Timestamp of the most recent (re-)send of this OTP to the user. */
    private LocalDateTime lastSentAt;

    /** The user to whom this OTP was issued (many-to-one, lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * JPA lifecycle callback invoked before the entity is first persisted.
     * Initialises verification state: not yet verified, zero attempts,
     * and records the current time as {@link #lastSentAt}.
     */
    @PrePersist
    protected void onCreateOTP() {
        this.verified = false;
        this.attemptCount = 0;
        this.lastSentAt = LocalDateTime.now();
    }
}
