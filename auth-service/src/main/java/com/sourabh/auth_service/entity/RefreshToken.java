package com.sourabh.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a refresh token stored in the
 * {@code refresh_token} database table.
 *
 * <p>Each token is linked to a specific user via {@code userUuid}, carries
 * an expiry date, and a revocation flag.  The auth-service uses a
 * token-rotation pattern: when a refresh token is exchanged for a new pair,
 * the old token is marked as revoked.</p>
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique, randomly generated UUID token value. */
    @Column(nullable = false, unique = true)
    private String token;

    /** UUID of the user who owns this refresh token. */
    @Column(nullable = false)
    private String userUuid;

    /** Date/time after which this token is no longer valid. */
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    /** Whether this token has been revoked (logout or rotation). */
    @Column(nullable = false)
    private boolean revoked;
}
