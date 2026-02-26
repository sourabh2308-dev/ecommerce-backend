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

// Data Repository - Provides database access via Spring Data JPA
/**
 * DATA ACCESS OBJECT - Database Query Interface
 * 
 * Extends JpaRepository to provide:
 *   - CRUD operations (Create, Read, Update, Delete)
 *   - Pagination and sorting (@Query custom methods)
 *   - Soft-delete support (isDeleted flag)
 * 
 * Spring Data JPA dynamically generates SQL from method names.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUuidAndIsDeletedFalse(String uuid);

    Page<Order> findByIsDeletedFalse(Pageable pageable);

    Page<Order> findByBuyerUuidAndIsDeletedFalse(
            String buyerUuid,
            Pageable pageable);

    /**


     * CUSTOM DATABASE QUERY


     * 


     * This method executes a custom JPQL or native SQL query against the database.


     * 


     * @Query annotation allows writing complex queries beyond Spring Data naming conventions.


     * - JPQL queries use entity names and field names (database-independent)


     * - Native queries use actual table/column names (database-specific SQL)


     * - :paramName binds method parameters to query


     * - ?1, ?2 for positional parameters


     * 


     * WHY CUSTOM QUERY:


     * - Complex joins across multiple tables


     * - Aggregations (COUNT, SUM, AVG, GROUP BY)


     * - Subqueries or conditional logic


     * - Performance optimization (specific columns, indexes)


     * 


     * Spring Data auto-implements this method at runtime.


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

    long countByStatus(OrderStatus status);

    long countByIsDeletedFalse();

    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.isDeleted = false")
    double sumTotalRevenue();

    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.sellerUuid = :sellerUuid AND o.status = :status AND o.isDeleted = false")
    long countBySellerAndStatus(@Param("sellerUuid") String sellerUuid, @Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o JOIN o.items i WHERE i.sellerUuid = :sellerUuid AND o.isDeleted = false")
    double sumRevenueForSeller(@Param("sellerUuid") String sellerUuid);

    /** Find unpaid orders older than threshold for auto-cancellation */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdBefore);

    /** Find sub-orders for a parent order */
    List<Order> findByParentOrderUuidAndIsDeletedFalse(String parentOrderUuid);

    /** Find all orders in a group (main + all sub-orders) */
    List<Order> findByOrderGroupIdAndIsDeletedFalse(String orderGroupId);

    /** Find orders by type */
    Page<Order> findByOrderTypeAndIsDeletedFalse(com.sourabh.order_service.entity.OrderType orderType, Pageable pageable);
}
