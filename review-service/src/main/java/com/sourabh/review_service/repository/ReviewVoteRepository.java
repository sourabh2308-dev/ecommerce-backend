package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ReviewVote} entities.
 *
 * <p>Provides finder and aggregation queries used to manage helpfulness
 * votes on reviews. Aggregate counts are used when building
 * {@link com.sourabh.review_service.dto.ReviewResponse} objects.
 *
 * @see ReviewVote
 */
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    /**
     * Finds an existing vote by a specific voter on a specific review.
     * Used to determine whether the voter has already voted so the vote
     * can be toggled rather than duplicated.
     *
     * @param reviewId  the primary key of the review
     * @param voterUuid the UUID of the voter
     * @return an {@link Optional} containing the vote, or empty if none exists
     */
    Optional<ReviewVote> findByReviewIdAndVoterUuid(Long reviewId, String voterUuid);

    /**
     * Counts the number of "helpful" votes for a review.
     *
     * @param reviewId the primary key of the review
     * @return the count of votes where {@code helpful = true}
     */
    @Query("SELECT COUNT(v) FROM ReviewVote v WHERE v.review.id = :reviewId AND v.helpful = true")
    long countHelpfulByReviewId(@Param("reviewId") Long reviewId);

    /**
     * Counts the number of "not helpful" votes for a review.
     *
     * @param reviewId the primary key of the review
     * @return the count of votes where {@code helpful = false}
     */
    @Query("SELECT COUNT(v) FROM ReviewVote v WHERE v.review.id = :reviewId AND v.helpful = false")
    long countNotHelpfulByReviewId(@Param("reviewId") Long reviewId);
}
