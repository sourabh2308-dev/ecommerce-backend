package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    List<ShipmentTracking> findByOrderUuidOrderByEventTimeAsc(String orderUuid);
}
