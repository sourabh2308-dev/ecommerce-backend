package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUuid(String uuid);
}
