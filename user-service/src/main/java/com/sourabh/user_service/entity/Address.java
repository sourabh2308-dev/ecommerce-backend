package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * JPA entity representing a physical address associated with a {@link User}.
 * <p>
 * Each user may have multiple addresses (e.g. Home, Office). Exactly one
 * address can be marked as the default shipping destination via
 * {@code isDefault}. A public UUID is auto-generated on first persist to
 * serve as an external-facing identifier so that the database surrogate
 * key is never exposed through the API.
 * </p>
 *
 * <p>Mapped to the {@code addresses} table. Inherits audit timestamps
 * ({@code createdAt}, {@code updatedAt}) from {@link BaseAuditEntity}.</p>
 *
 * @see User
 * @see BaseAuditEntity
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseAuditEntity {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Publicly-exposed unique identifier; auto-generated in {@link #onCreateAddress()}. */
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    /** The user who owns this address (many-to-one, lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Human-readable label such as "Home" or "Office". */
    private String label;

    /** Full name of the recipient at this address. */
    @Column(nullable = false)
    private String fullName;

    /** Contact phone number (max 15 characters). */
    @Column(nullable = false, length = 15)
    private String phone;

    /** Primary street / building address line. */
    @Column(nullable = false)
    private String addressLine1;

    /** Optional secondary address line (apartment, suite, etc.). */
    private String addressLine2;

    /** City name. */
    @Column(nullable = false)
    private String city;

    /** State or province. */
    @Column(nullable = false)
    private String state;

    /** Postal / ZIP code (max 10 characters). */
    @Column(nullable = false, length = 10)
    private String pincode;

    /** Whether this address is the user's default shipping address. */
    @Column(nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /**
     * JPA lifecycle callback invoked before the entity is first persisted.
     * Generates a random UUID if one has not already been assigned.
     */
    @PrePersist
    protected void onCreateAddress() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
