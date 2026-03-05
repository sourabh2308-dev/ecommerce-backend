package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.FlashDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link FlashDeal} entities.
 * <p>
 * Provides CRUD operations and custom queries for retrieving active deals,
 * seller-specific deals, and expired deals pending deactivation.
 * </p>
 */
public interface FlashDealRepository extends JpaRepository<FlashDeal, Long> {

    /**
     * Finds a flash deal by its public UUID.
     *
     * @param uuid the universally unique identifier
     * @return an {@link Optional} containing the matching deal, or empty
     */
    Optional<FlashDeal> findByUuid(String uuid);

    /**
     * Finds the currently active flash deal for a given product.
     * A deal is considered active when {@code isActive = true} and the current
     * time falls within its {@code [startTime, endTime]} window.
     *
     * @param productUuid UUID of the product
     * @param now         current timestamp for the time-window check
     * @return an {@link Optional} containing the active deal, or empty
     */
    @Query("SELECT f FROM FlashDeal f WHERE f.product.uuid = :productUuid " +
           "AND f.isActive = true AND f.startTime <= :now AND f.endTime >= :now")
    Optional<FlashDeal> findActiveByProductUuid(String productUuid, LocalDateTime now);

    /**
     * Returns all currently active flash deals, ordered by end time ascending
     * (soonest-ending deals first).
     *
     * @param now current timestamp for the time-window check
     * @return list of active deals
     */
    @Query("SELECT f FROM FlashDeal f WHERE f.isActive = true " +
           "AND f.startTime <= :now AND f.endTime >= :now ORDER BY f.endTime ASC")
    List<FlashDeal> findAllActive(LocalDateTime now);

    /**
     * Returns all flash deals created by a specific seller, newest first.
     *
     * @param sellerUuid UUID of the seller
     * @return list of the seller's deals
     */
    List<FlashDeal> findBySellerUuidOrderByCreatedAtDesc(String sellerUuid);

    /**
     * Finds all flash deals that have expired (end time in the past) but are
     * still marked as active. Used by {@code ExpireFlashDealsScheduler} for
     * automated deactivation.
     *
     * @param endTime reference timestamp (typically {@code LocalDateTime.now()})
     * @return list of expired but still-active deals
     */
    List<FlashDeal> findByEndTimeBeforeAndIsActiveTrue(LocalDateTime endTime);
}
