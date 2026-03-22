package com.sourabh.order_service.service;

import com.sourabh.order_service.dto.request.CreateCouponRequest;
import com.sourabh.order_service.dto.response.CouponResponse;
import com.sourabh.order_service.dto.response.CouponValidationResponse;

import java.util.List;

public interface CouponService {

    CouponResponse createCoupon(CreateCouponRequest request);

    CouponValidationResponse validateCoupon(String code, Double orderAmount, String buyerUuid);

    void recordUsage(String code, String buyerUuid, String orderUuid);

    String deactivateCoupon(String code);

    List<CouponResponse> listAll();
}
