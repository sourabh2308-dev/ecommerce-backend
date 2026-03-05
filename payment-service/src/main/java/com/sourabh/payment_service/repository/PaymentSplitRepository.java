package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.PaymentSplit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for {@link PaymentSplit} entities.
 *
 * <p>Provides standard CRUD operations along with a rich set of
 * aggregation queries used by the seller dashboard and the admin
 * dashboard.  All {@code @Query} methods use JPQL and operate on
 * entity field names rather than raw column names, keeping the
 * repository database-agnostic.
 *
 * <h3>Key query categories</h3>
 * <ul>
 *   <li><strong>Seller-scoped</strong> — filter by {@code sellerUuid}
 *       and aggregate completed/pending payouts and order counts.</li>
 *   <li><strong>Platform-wide</strong> — aggregate gross revenue,
 *       platform fees, delivery fees, seller payouts, order counts
 *       and active seller counts across all completed splits.</li>
 * </ul>
 */
public interface PaymentSplitRepository extends JpaRepository<PaymentSplit, Long> {

    /**
     * Returns all splits belonging to a specific payment.
     *
     * @param paymentUuid the parent payment UUID
     * @return list of splits (may be empty)
     */
    List<PaymentSplit> findByPaymentUuid(String paymentUuid);

    /**
     * Returns a paginated list of splits for a given seller.
     *
     * @param sellerUuid the seller UUID
     * @param pageable   pagination and sort parameters
     * @return a page of payment splits
     */
    Page<PaymentSplit> findBySellerUuid(String sellerUuid, Pageable pageable);

    /**
     * Sums the seller payout for all <em>completed</em> splits belonging
     * to the specified seller.  Returns {@code 0} when no rows match.
     *
     * @param sellerUuid the seller UUID
     * @return total completed payout amount
     */
    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Double sumSellerPayout(@Param("sellerUuid") String sellerUuid);

    /**
     * Sums the seller payout for all <em>pending</em> splits belonging
     * to the specified seller.  Returns {@code 0} when no rows match.
     *
     * @param sellerUuid the seller UUID
     * @return total pending payout amount
     */
    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'PENDING'")
    Double sumSellerPendingPayout(@Param("sellerUuid") String sellerUuid);

    /**
     * Counts distinct order UUIDs across a seller's completed splits,
     * giving the total number of orders the seller has fulfilled.
     *
     * @param sellerUuid the seller UUID
     * @return number of completed orders
     */
    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Long countSellerOrders(@Param("sellerUuid") String sellerUuid);

    /**
     * Platform-wide sum of platform fees across all completed splits.
     *
     * @return total platform fee revenue
     */
    @Query("SELECT COALESCE(SUM(ps.platformFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalPlatformFees();

    /**
     * Platform-wide sum of delivery fees across all completed splits.
     *
     * @return total delivery fee revenue
     */
    @Query("SELECT COALESCE(SUM(ps.deliveryFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalDeliveryFees();

    /**
     * Platform-wide sum of seller payouts across all completed splits.
     *
     * @return total amount paid out to sellers
     */
    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalSellerPayouts();

    /**
     * Platform-wide sum of item amounts (gross revenue) across all
     * completed splits.
     *
     * @return total gross item revenue
     */
    @Query("SELECT COALESCE(SUM(ps.itemAmount), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalItemAmount();

    /**
     * Counts the total number of distinct completed orders across the
     * entire platform.
     *
     * @return number of completed orders
     */
    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countTotalCompletedOrders();

    /**
     * Counts the total number of unique sellers that have at least one
     * completed split, indicating active participation on the platform.
     *
     * @return number of active sellers
     */
    @Query("SELECT COUNT(DISTINCT ps.sellerUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countActiveSellers();
}
