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

@RestController
@RequestMapping("/api/product/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PutMapping("/{productUuid}/restock")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> restock(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.restock(productUuid, sellerUuid, request));
    }

    @PutMapping("/{productUuid}/adjust")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> adjustStock(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(productUuid, sellerUuid, request));
    }

    @GetMapping("/{productUuid}/history")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<PageResponse<StockMovementResponse>> getStockHistory(
            @PathVariable String productUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getStockHistory(productUuid, page, size));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<String>> getLowStock(
            @RequestHeader("X-User-UUID") String sellerUuid,
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(inventoryService.getLowStockProducts(sellerUuid, threshold));
    }
}
