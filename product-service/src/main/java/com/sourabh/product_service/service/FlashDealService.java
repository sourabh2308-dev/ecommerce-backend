package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.FlashDealRequest;
import com.sourabh.product_service.dto.response.FlashDealResponse;

import java.util.List;

public interface FlashDealService {

    FlashDealResponse createDeal(String sellerUuid, FlashDealRequest request);

    List<FlashDealResponse> getActiveDeals();

    List<FlashDealResponse> getMyDeals(String sellerUuid);

    String cancelDeal(String dealUuid, String sellerUuid);
}
