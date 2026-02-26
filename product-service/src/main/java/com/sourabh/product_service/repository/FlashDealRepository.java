package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.FlashDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashDealRepository extends JpaRepository<FlashDeal, Long> {

    Optional<FlashDeal> findByUuid(String uuid);

    @Query("SELECT f FROM FlashDeal f WHERE f.product.uuid = :productUuid " +
           "AND f.isActive = true AND f.startTime <= :now AND f.endTime >= :now")
    Optional<FlashDeal> findActiveByProductUuid(String productUuid, LocalDateTime now);

    @Query("SELECT f FROM FlashDeal f WHERE f.isActive = true " +
           "AND f.startTime <= :now AND f.endTime >= :now ORDER BY f.endTime ASC")
    List<FlashDeal> findAllActive(LocalDateTime now);

    List<FlashDeal> findBySellerUuidOrderByCreatedAtDesc(String sellerUuid);

    /** Find expired flash deals for auto-deactivation */
    List<FlashDeal> findByEndTimeBeforeAndIsActiveTrue(LocalDateTime endTime);
}
