package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    int countByCouponIdAndBuyerUuid(Long couponId, String buyerUuid);
}
