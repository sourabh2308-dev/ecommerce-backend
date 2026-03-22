package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUuid(String uuid);

    boolean existsByOrderUuid(String orderUuid);

    Optional<Payment> findByOrderUuid(String orderUuid);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    Page<Payment> findByBuyerUuid(String buyerUuid, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Payment p " +
           "WHERE EXISTS (SELECT 1 FROM PaymentSplit ps WHERE ps.paymentUuid = p.uuid AND ps.sellerUuid = :sellerUuid)")
    Page<Payment> findBySellerUuid(@Param("sellerUuid") String sellerUuid, Pageable pageable);
}
