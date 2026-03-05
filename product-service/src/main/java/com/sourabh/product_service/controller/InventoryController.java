package com.sourabh.product_service.controller;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.StockUpdateRequest;
import com.sourabh.product_service.dto.response.StockMovementResponse;
import com.sourabh.product_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for inventory management operations.
 * <p>
 * Provides endpoints for restocking, manual stock adjustments, stock-movement
 * history retrieval, and identification of low-stock products. All mutating
 * operations are restricted to the {@code SELLER} role, while history may also
 * be accessed by {@code ADMIN} users.
 * </p>
 *
 * <p>Base path: {@code /api/product/inventory}</p>
 */
@RestController
@RequestMapping("/api/product/inventory")
@RequiredArgsConstructor
public class InventoryController {

    /** Service encapsulating inventory business logic. */
    private final InventoryService inventoryService;

    /**
     * Adds stock to a product (restock operation).
     *
     * @param productUuid UUID of the product to restock
     * @param sellerUuid  UUID of the authenticated seller
     * @param request     validated payload containing the quantity to add
     * @return confirmation message with the updated stock level
     */
    @PutMapping("/{productUuid}/restock")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> restock(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.restock(productUuid, sellerUuid, request));
    }

    /**
     * Performs a manual stock adjustment (positive or negative correction).
     *
     * @param productUuid UUID of the product to adjust
     * @param sellerUuid  UUID of the authenticated seller
     * @param request     validated payload containing the adjustment quantity
     * @return confirmation message with the updated stock level
     */
    @PutMapping("/{productUuid}/adjust")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> adjustStock(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(productUuid, sellerUuid, request));
    }

    /**
     * Retrieves a paginated history of stock movements for a product.
     *
     * @param productUuid UUID of the product
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 20)
     * @return paginated list of stock movement records
     */
    @GetMapping("/{productUuid}/history")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<PageResponse<StockMovementResponse>> getStockHistory(
            @PathVariable String productUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getStockHistory(productUuid, page, size));
    }

    /**
     * Returns UUIDs of products whose current stock is at or below the given threshold.
     *
     * @param sellerUuid UUID of the authenticated seller
     * @param threshold  stock level threshold (default 10)
     * @return list of product UUIDs with low stock
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<String>> getLowStock(
            @RequestHeader("X-User-UUID") String sellerUuid,
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(inventoryService.getLowStockProducts(sellerUuid, threshold));
    }
}
