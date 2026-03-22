package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    Optional<ReviewVote> findByReviewIdAndVoterUuid(Long reviewId, String voterUuid);

    @Query("SELECT COUNT(v) FROM ReviewVote v WHERE v.review.id = :reviewId AND v.helpful = true")
    long countHelpfulByReviewId(@Param("reviewId") Long reviewId);

    @Query("SELECT COUNT(v) FROM ReviewVote v WHERE v.review.id = :reviewId AND v.helpful = false")
    long countNotHelpfulByReviewId(@Param("reviewId") Long reviewId);
}
