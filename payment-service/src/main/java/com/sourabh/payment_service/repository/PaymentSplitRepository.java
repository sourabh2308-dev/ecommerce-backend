package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.PaymentSplit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentSplitRepository extends JpaRepository<PaymentSplit, Long> {

    List<PaymentSplit> findByPaymentUuid(String paymentUuid);

    Page<PaymentSplit> findBySellerUuid(String sellerUuid, Pageable pageable);

    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Double sumSellerPayout(@Param("sellerUuid") String sellerUuid);

    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'PENDING'")
    Double sumSellerPendingPayout(@Param("sellerUuid") String sellerUuid);

    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.sellerUuid = :sellerUuid AND ps.status = 'COMPLETED'")
    Long countSellerOrders(@Param("sellerUuid") String sellerUuid);

    @Query("SELECT COALESCE(SUM(ps.platformFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalPlatformFees();

    @Query("SELECT COALESCE(SUM(ps.deliveryFee), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalDeliveryFees();

    @Query("SELECT COALESCE(SUM(ps.sellerPayout), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalSellerPayouts();

    @Query("SELECT COALESCE(SUM(ps.itemAmount), 0) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Double sumTotalItemAmount();

    @Query("SELECT COUNT(DISTINCT ps.orderUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countTotalCompletedOrders();

    @Query("SELECT COUNT(DISTINCT ps.sellerUuid) FROM PaymentSplit ps WHERE ps.status = 'COMPLETED'")
    Long countActiveSellers();
}
