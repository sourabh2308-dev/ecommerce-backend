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

public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Long> {

    Page<LoyaltyPoint> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoint lp WHERE lp.userUuid = :userUuid")
    int getBalance(@Param("userUuid") String userUuid);

    Optional<LoyaltyPoint> findTopByUserUuidOrderByCreatedAtDesc(String userUuid);

    /** Find earned points that haven't been redeemed and are older than threshold for expiration */
    @Query("SELECT lp FROM LoyaltyPoint lp WHERE lp.type IN ('EARNED_ORDER', 'EARNED_REVIEW', 'EARNED_REFERRAL') AND lp.createdAt < :expiryThreshold")
    List<LoyaltyPoint> findPointsToExpire(@Param("expiryThreshold") LocalDateTime expiryThreshold);
}
