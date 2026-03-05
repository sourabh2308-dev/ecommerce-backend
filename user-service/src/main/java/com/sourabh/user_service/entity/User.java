package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core JPA entity representing a platform user account.
 * <p>
 * Supports three {@link Role roles}: {@code BUYER}, {@code SELLER},
 * and {@code ADMIN}. The account progresses through several
 * {@link UserStatus lifecycle states} — from initial registration
 * and email verification through to full activation, and optionally
 * blocking or soft-deletion by an administrator.
 * </p>
 *
 * <p>Passwords are stored as BCrypt hashes. A public UUID is
 * auto-generated on first persist via {@link #onCreateUser()} so
 * that the database surrogate key is never leaked through the API.</p>
 *
 * <p>Mapped to the {@code users} table with unique constraints on
 * {@code email} and {@code uuid}. Inherits audit timestamps from
 * {@link BaseAuditEntity}.</p>
 *
 * @see Role
 * @see UserStatus
 * @see BaseAuditEntity
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditEntity {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Publicly-exposed unique identifier; generated in {@link #onCreateUser()}. */
    @Column(nullable = false, updatable = false)
    private String uuid;

    /** User's first (given) name. */
    @Column(nullable = false)
    private String firstName;

    /** User's last (family) name. */
    @Column(nullable = false)
    private String lastName;

    /** User's email address — used as the login credential. Must be unique. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Optional phone number (max 15 characters, E.164 format recommended). */
    @Column(length = 15)
    private String phoneNumber;

    /** BCrypt-hashed password. */
    @Column(nullable = false)
    private String password;

    /** Access-control role assigned to this user. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Current lifecycle status of the account. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    /** Whether the user has completed email OTP verification. */
    @Column(nullable = false)
    private boolean emailVerified;

    /** Whether the seller account has been approved by an administrator. */
    @Column(nullable = false)
    private boolean isApproved;

    /** Soft-delete flag; {@code true} means the account is logically removed. */
    @Column(nullable = false)
    private boolean isDeleted;

    /** Timestamp of the user's most recent successful login. */
    private LocalDateTime lastLoginAt;

    /**
     * JPA lifecycle callback invoked before the entity is first persisted.
     * <ul>
     *   <li>Generates a random UUID.</li>
     *   <li>Sets {@code emailVerified}, {@code isApproved}, and
     *       {@code isDeleted} to {@code false}.</li>
     *   <li>Defaults {@link #status} to
     *       {@link UserStatus#PENDING_VERIFICATION} if not already set.</li>
     * </ul>
     */
    @PrePersist
    protected void onCreateUser() {
        this.uuid = UUID.randomUUID().toString();
        this.emailVerified = false;
        this.isApproved = false;
        this.isDeleted = false;

        if (this.status == null) {
            this.status = UserStatus.PENDING_VERIFICATION;
        }
    }
}
