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
    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Double sumSellerPayout(@Param("sellerUuid") String sellerUuid);

    /** Total seller payout that is still pending */
    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'PENDING'")
    Double sumSellerPendingPayout(@Param("sellerUuid") String sellerUuid);

    /** Count of orders for a seller (distinct order UUIDs) */
    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Long countSellerOrders(@Param("sellerUuid") String sellerUuid);

    /** Total platform fees collected (all completed splits) */
    @Query("SELECT COALESCE(SUM(ps.platformFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalPlatformFees();

    /** Total delivery fees collected (all completed splits) */
    @Query("SELECT COALESCE(SUM(ps.deliveryFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalDeliveryFees();

    /** Total seller payouts (all completed splits) */
    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalSellerPayouts();

    /** Total item amounts (gross revenue, all completed splits) */
    @Query("SELECT COALESCE(SUM(ps.itemAmount), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalItemAmount();

    /** Count total completed orders (distinct) */
    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countTotalCompletedOrders();

    /** Count total unique sellers with completed splits */
    @Query("SELECT COUNT(DISTINCT ps.sellerUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countActiveSellers();
}
