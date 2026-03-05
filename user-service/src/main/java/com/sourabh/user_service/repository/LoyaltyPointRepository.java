package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.LoyaltyPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link LoyaltyPoint} entities.
 * <p>
 * Provides standard CRUD operations, balance aggregation, and
 * queries for transaction history and point-expiration scheduling.
 * </p>
 *
 * @see LoyaltyPoint
 */
public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Long> {

    /**
     * Returns a paginated transaction history for the given user,
     * ordered newest-first.
     *
     * @param userUuid the user's UUID
     * @param pageable pagination parameters
     * @return page of loyalty-point transactions
     */
    Page<LoyaltyPoint> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    /**
     * Computes the current loyalty-points balance for the user by
     * summing all transaction point values.
     *
     * @param userUuid the user's UUID
     * @return current points balance (may be zero)
     */
    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoint lp WHERE lp.userUuid = :userUuid")
    int getBalance(@Param("userUuid") String userUuid);

    /**
     * Retrieves the most recent loyalty-point transaction for the user.
     *
     * @param userUuid the user's UUID
     * @return the latest transaction, if any
     */
    Optional<LoyaltyPoint> findTopByUserUuidOrderByCreatedAtDesc(String userUuid);

    /**
     * Finds earned points (ORDER, REVIEW, REFERRAL) that were created
     * before the given threshold date and are candidates for expiration.
     *
     * @param expiryThreshold cut-off timestamp; transactions older than
     *                        this are eligible for expiry
     * @return list of expirable loyalty-point records
     */
    @Query("SELECT lp FROM LoyaltyPoint lp WHERE lp.type IN ('EARNED_ORDER', 'EARNED_REVIEW', 'EARNED_REFERRAL') AND lp.createdAt < :expiryThreshold")
    List<LoyaltyPoint> findPointsToExpire(@Param("expiryThreshold") LocalDateTime expiryThreshold);
}
