package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ShipmentTracking} entities.
 *
 * <p>Provides chronological retrieval of all tracking events associated
 * with a given order UUID.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ShipmentTracking
 */
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    /**
     * Retrieves every tracking event for the specified order, sorted by
     * {@code eventTime} ascending (oldest first).
     *
     * @param orderUuid the order UUID
     * @return chronologically ordered list of tracking events
     */
    List<ShipmentTracking> findByOrderUuidOrderByEventTimeAsc(String orderUuid);
}
