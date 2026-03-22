package com.sourabh.product_service.service;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.StockUpdateRequest;
import com.sourabh.product_service.dto.response.StockMovementResponse;

import java.util.List;

public interface InventoryService {

    String restock(String productUuid, String sellerUuid, StockUpdateRequest request);

    String adjustStock(String productUuid, String sellerUuid, StockUpdateRequest request);

    PageResponse<StockMovementResponse> getStockHistory(String productUuid, int page, int size);

    List<String> getLowStockProducts(String sellerUuid, int threshold);
}
