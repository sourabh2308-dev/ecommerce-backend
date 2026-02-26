package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductUuidOrderByCreatedAtDesc(String productUuid, Pageable pageable);

    List<StockMovement> findTop10ByProductUuidOrderByCreatedAtDesc(String productUuid);
}
