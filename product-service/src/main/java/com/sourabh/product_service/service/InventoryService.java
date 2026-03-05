package com.sourabh.product_service.service;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.StockUpdateRequest;
import com.sourabh.product_service.dto.response.StockMovementResponse;

import java.util.List;

/**
 * Service interface for inventory and stock management operations.
 *
 * <p>Provides seller-facing stock control operations including restocking,
 * manual adjustments, movement-history tracking, and low-stock alerts.
 * Every stock change is recorded as a {@link StockMovementResponse} for
 * full audit trail support.</p>
 *
 * @see com.sourabh.product_service.service.impl.InventoryServiceImpl
 * @see com.sourabh.product_service.entity.StockMovement
 */
public interface InventoryService {

    /**
     * Adds inventory to a product (restock operation).
     *
     * <p>Increases the product's stock by the quantity specified in the
     * request and records a RESTOCK movement.</p>
     *
     * @param productUuid the UUID of the product to restock
     * @param sellerUuid  the UUID of the seller (must own the product)
     * @param request     contains the quantity to add and an optional reference
     * @return confirmation message with the new stock level
     * @throws com.sourabh.product_service.exception.ProductNotFoundException
     *         if the product does not exist
     * @throws RuntimeException if the seller does not own the product
     */
    String restock(String productUuid, String sellerUuid, StockUpdateRequest request);

    /**
     * Manually sets the stock level for a product (adjustment).
     *
     * <p>Overwrites the current stock value and records an ADJUSTMENT movement.</p>
     *
     * @param productUuid the UUID of the product
     * @param sellerUuid  the UUID of the seller (must own the product)
     * @param request     contains the new absolute stock quantity and an optional reference
     * @return confirmation message with the adjusted stock level
     */
    String adjustStock(String productUuid, String sellerUuid, StockUpdateRequest request);

    /**
     * Returns a paginated history of stock movements for a product.
     *
     * @param productUuid the UUID of the product
     * @param page        zero-based page index
     * @param size        number of records per page
     * @return paginated {@link StockMovementResponse} list ordered by date descending
     */
    PageResponse<StockMovementResponse> getStockHistory(String productUuid, int page, int size);

    /**
     * Finds all products belonging to a seller whose current stock is
     * at or below the given threshold.
     *
     * @param sellerUuid the UUID of the seller
     * @param threshold  stock level threshold (inclusive)
     * @return list of product UUIDs with low stock
     */
    List<String> getLowStockProducts(String sellerUuid, int threshold);
}
