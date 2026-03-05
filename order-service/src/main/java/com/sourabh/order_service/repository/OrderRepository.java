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

/**
 * Spring Data JPA repository for {@link Order} entities.
 *
 * <p>Provides standard CRUD operations plus custom finders for soft-deleted
 * filtering, seller-scoped queries, aggregate counts, revenue calculations,
 * order-group look-ups, and scheduler-driven queries (e.g. auto-cancellation
 * of unpaid orders).</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Order
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds a non-deleted order by its UUID.
     *
     * @param uuid the order UUID
     * @return an {@link Optional} containing the order, or empty if not found
     *         or soft-deleted
     */
    Optional<Order> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * Returns a page of all non-deleted orders.
     *
     * @param pageable pagination and sorting information
     * @return page of non-deleted {@link Order} entities
     */
    Page<Order> findByIsDeletedFalse(Pageable pageable);

    /**
     * Returns a page of non-deleted orders placed by the specified buyer.
     *
     * @param buyerUuid the buyer's UUID
     * @param pageable  pagination and sorting information
     * @return page of matching {@link Order} entities
     */
    Page<Order> findByBuyerUuidAndIsDeletedFalse(
            String buyerUuid,
            Pageable pageable);

    /**
     * Finds distinct non-deleted orders that contain at least one item
     * belonging to the specified seller.
     *
     * @param sellerUuid the seller's UUID
     * @param pageable   pagination and sorting information
     * @return page of matching {@link Order} entities
     */
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

    /**
     * Counts orders with the given status (regardless of soft-delete flag).
     *
     * @param status the {@link OrderStatus} to count
     * @return the number of matching orders
     */
    long countByStatus(OrderStatus status);

    /**
     * Counts all non-deleted orders in the system.
     *
     * @return total non-deleted order count
     */
    long countByIsDeletedFalse();

    /**
     * Calculates the sum of {@code totalAmount} for all non-deleted orders.
     *
     * @return cumulative revenue, or {@code 0} if no orders exist
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.isDeleted = false")
    double sumTotalRevenue();

    /**
     * Counts non-deleted orders for a given seller and status combination.
     *
     * @param sellerUuid the seller's UUID
     * @param status     the {@link OrderStatus} to filter by
     * @return the number of matching orders
     */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.sellerUuid = :sellerUuid AND o.status = :status AND o.isDeleted = false")
    long countBySellerAndStatus(@Param("sellerUuid") String sellerUuid, @Param("status") OrderStatus status);

    /**
     * Calculates the sum of {@code totalAmount} for non-deleted orders
     * containing items belonging to the specified seller.
     *
     * @param sellerUuid the seller's UUID
     * @return seller's cumulative revenue, or {@code 0} if no orders exist
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o JOIN o.items i WHERE i.sellerUuid = :sellerUuid AND o.isDeleted = false")
    double sumRevenueForSeller(@Param("sellerUuid") String sellerUuid);

    /**
     * Finds orders with the given status that were created before the
     * specified timestamp. Used by
     * {@link com.sourabh.order_service.scheduler.AutoCancelUnpaidOrdersScheduler}
     * to identify stale unpaid orders.
     *
     * @param status        the order status to match
     * @param createdBefore the cutoff timestamp
     * @return list of matching orders
     */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdBefore);

    /**
     * Finds non-deleted sub-orders whose parent order has the given UUID.
     *
     * @param parentOrderUuid UUID of the parent order
     * @return list of sub-orders
     */
    List<Order> findByParentOrderUuidAndIsDeletedFalse(String parentOrderUuid);

    /**
     * Finds all non-deleted orders sharing the same order group identifier
     * (main order plus all sub-orders).
     *
     * @param orderGroupId the shared group ID
     * @return list of grouped orders
     */
    List<Order> findByOrderGroupIdAndIsDeletedFalse(String orderGroupId);

    /**
     * Returns a page of non-deleted orders filtered by {@link com.sourabh.order_service.entity.OrderType}.
     *
     * @param orderType the order type to filter by
     * @param pageable  pagination and sorting information
     * @return page of matching orders
     */
    Page<Order> findByOrderTypeAndIsDeletedFalse(com.sourabh.order_service.entity.OrderType orderType, Pageable pageable);
}
