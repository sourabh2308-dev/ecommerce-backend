package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}
