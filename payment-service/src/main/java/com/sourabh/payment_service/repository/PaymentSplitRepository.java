package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.PaymentSplit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Data Repository - Provides database access via Spring Data JPA
public interface PaymentSplitRepository extends JpaRepository<PaymentSplit, Long> {

    List<PaymentSplit> findByPaymentUuid(String paymentUuid);

    Page<PaymentSplit> findBySellerUuid(String sellerUuid, Pageable pageable);

    /** Total seller payout (completed splits only) */
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

    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Double sumSellerPayout(@Param("sellerUuid") String sellerUuid);

    /** Total seller payout that is still pending */
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

    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'PENDING'")
    Double sumSellerPendingPayout(@Param("sellerUuid") String sellerUuid);

    /** Count of orders for a seller (distinct order UUIDs) */
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

    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Long countSellerOrders(@Param("sellerUuid") String sellerUuid);

    /** Total platform fees collected (all completed splits) */
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

    @Query("SELECT COALESCE(SUM(ps.platformFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalPlatformFees();

    /** Total delivery fees collected (all completed splits) */
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

    @Query("SELECT COALESCE(SUM(ps.deliveryFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalDeliveryFees();

    /** Total seller payouts (all completed splits) */
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

    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalSellerPayouts();

    /** Total item amounts (gross revenue, all completed splits) */
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

    @Query("SELECT COALESCE(SUM(ps.itemAmount), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalItemAmount();

    /** Count total completed orders (distinct) */
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

    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countTotalCompletedOrders();

    /** Count total unique sellers with completed splits */
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

    @Query("SELECT COUNT(DISTINCT ps.sellerUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countActiveSellers();
}
