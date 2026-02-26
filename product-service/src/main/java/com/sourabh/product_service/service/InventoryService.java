package com.sourabh.product_service.service;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.StockUpdateRequest;
import com.sourabh.product_service.dto.response.StockMovementResponse;

import java.util.List;

public interface InventoryService {

    /** Seller restocks a product */
    String restock(String productUuid, String sellerUuid, StockUpdateRequest request);

    /** Seller manually adjusts stock */
    String adjustStock(String productUuid, String sellerUuid, StockUpdateRequest request);

    /** Get stock movement history */
    PageResponse<StockMovementResponse> getStockHistory(String productUuid, int page, int size);

    /** Get all products with stock below threshold */
    List<String> getLowStockProducts(String sellerUuid, int threshold);
}
