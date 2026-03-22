package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    long countByStatus(OrderStatus status);

    long countByIsDeletedFalse();

    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.isDeleted = false")
    double sumTotalRevenue();

    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.sellerUuid = :sellerUuid AND o.status = :status AND o.isDeleted = false")
    long countBySellerAndStatus(@Param("sellerUuid") String sellerUuid, @Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o JOIN o.items i WHERE i.sellerUuid = :sellerUuid AND o.isDeleted = false")
    double sumRevenueForSeller(@Param("sellerUuid") String sellerUuid);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdBefore);

    List<Order> findByParentOrderUuidAndIsDeletedFalse(String parentOrderUuid);

    List<Order> findByOrderGroupIdAndIsDeletedFalse(String orderGroupId);

    Page<Order> findByOrderTypeAndIsDeletedFalse(com.sourabh.order_service.entity.OrderType orderType, Pageable pageable);
}
