package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Data Repository - Provides database access via Spring Data JPA
public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUuid(String uuid);

    boolean existsByOrderUuid(String orderUuid);

    Optional<Payment> findByOrderUuid(String orderUuid);

    Page<Payment> findByBuyerUuid(String buyerUuid, Pageable pageable);
}
