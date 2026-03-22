package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    boolean existsByCode(String code);

    List<Coupon> findByValidUntilBeforeAndIsActiveTrue(LocalDateTime validUntil);
}
