package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUuidAndIsDeletedFalse(String uuid);

    Page<Order> findByIsDeletedFalse(Pageable pageable);

    Page<Order> findByBuyerUuidAndIsDeletedFalse(
            String buyerUuid,
            Pageable pageable);

    @Query("""
       SELECT DISTINCT o
       FROM Order o
       JOIN o.items i
       WHERE i.sellerUuid = :sellerUuid
       AND o.isDeleted = false
       """)
    Page<Order> findOrdersBySeller(
            @Param("sellerUuid") String sellerUuid,
            Pageable pageable);

}
