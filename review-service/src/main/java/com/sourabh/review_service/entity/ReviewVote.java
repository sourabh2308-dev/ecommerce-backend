package com.sourabh.review_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity recording a helpfulness vote on a {@link Review}.
 *
 * <p>Each user may cast exactly one vote per review (enforced by a unique
 * constraint on {@code (review_id, voter_uuid)}). The vote can be toggled
 * between helpful and not-helpful by submitting a new vote.
 *
 * <h3>Table</h3>
 * Mapped to the {@code review_vote} table in PostgreSQL with a composite
 * unique constraint preventing duplicate votes.
 *
 * @see Review#getVotes()
 * @see com.sourabh.review_service.repository.ReviewVoteRepository
 */
@Entity
@Table(name = "review_vote",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "voter_uuid"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewVote {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The review being voted on.
     * Lazily fetched to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /** UUID of the user who cast this vote. */
    @Column(nullable = false)
    private String voterUuid;

    /**
     * Vote direction: {@code true} indicates the review was helpful,
     * {@code false} indicates it was not helpful.
     */
    @Column(nullable = false)
    private boolean helpful;

    /** Timestamp recording when the vote was cast or last updated. */
    @Column(nullable = false)
    private LocalDateTime votedAt;

    /**
     * JPA lifecycle callback that sets {@link #votedAt} to the current
     * time immediately before the entity is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.votedAt = LocalDateTime.now();
    }
}
