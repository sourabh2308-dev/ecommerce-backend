package com.sourabh.review_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a product review submitted by a buyer.
 *
 * <p>Each review is linked to a specific product and buyer, carries a star
 * rating (1–5) and an optional text comment. Reviews support soft-delete
 * via the {@link #isDeleted} flag so that an audit trail is preserved while
 * the review is hidden from public queries.
 *
 * <h3>Relationships</h3>
 * <ul>
 *   <li>{@link ReviewImage} — one-to-many; images attached by the buyer</li>
 *   <li>{@link ReviewVote} — one-to-many; helpful / not-helpful votes</li>
 * </ul>
 *
 * <h3>Table</h3>
 * Mapped to the default table name {@code review} in the PostgreSQL
 * {@code review_db} database.
 *
 * @see com.sourabh.review_service.repository.ReviewRepository
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    /** Auto-generated primary key (database sequence). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Externally-visible unique identifier (UUID string) used in REST
     * endpoints so that internal surrogate keys are never exposed.
     */
    @Column(unique = true)
    private String uuid;

    /** UUID of the product this review is associated with. */
    private String productUuid;

    /** UUID of the seller who listed the reviewed product. */
    private String sellerUuid;

    /** UUID of the buyer who authored the review. */
    private String buyerUuid;

    /**
     * Star rating from 1 (poor) to 5 (excellent).
     * Validated both at the DTO level and at the entity level.
     */
    @Min(1)
    @Max(5)
    private Integer rating;

    /**
     * Optional free-text comment written by the buyer.
     * Maximum length is 2 000 characters.
     */
    @Column(length = 2000)
    private String comment;

    /**
     * Soft-delete flag. When {@code true} the review is excluded from
     * all public queries but retained in the database for auditing.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    /**
     * {@code true} if the buyer had a delivered order containing the
     * reviewed product at the time the review was created.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean verifiedPurchase = false;

    /**
     * Images uploaded by the buyer to accompany this review.
     * Cascade-all ensures images are persisted/removed with the review.
     */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    /**
     * Helpfulness votes cast by other users on this review.
     * Cascade-all ensures votes are managed alongside the review.
     */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewVote> votes = new ArrayList<>();

    /** Timestamp recording when the review was first created. */
    private LocalDateTime createdAt;
}
