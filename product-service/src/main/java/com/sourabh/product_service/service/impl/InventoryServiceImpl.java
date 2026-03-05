package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.StockUpdateRequest;
import com.sourabh.product_service.dto.response.StockMovementResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.entity.StockMovement;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.repository.StockMovementRepository;
import com.sourabh.product_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link InventoryService} providing stock management
 * and movement-audit capabilities.
 *
 * <p>All stock mutations (restock, adjustment) are persisted alongside a
 * {@link StockMovement} record so that a full audit trail of inventory
 * changes is available. Methods enforce seller ownership before allowing
 * any modification.</p>
 *
 * @see InventoryService
 * @see StockMovementRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    /** Repository for product entity operations. */
    private final ProductRepository productRepository;

    /** Repository for stock-movement audit records. */
    private final StockMovementRepository stockMovementRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Adds the requested quantity to the current stock and persists
     * a RESTOCK movement entry.</p>
     */
    @Override
    @Transactional
    public String restock(String productUuid, String sellerUuid, StockUpdateRequest request) {
        Product product = findProductBySeller(productUuid, sellerUuid);
        int newStock = product.getStock() + request.getQuantity();
        product.setStock(newStock);
        productRepository.save(product);

        recordMovement(productUuid, StockMovement.MovementType.RESTOCK,
                request.getQuantity(), newStock, request.getReference());

        log.info("[INVENTORY] Restocked product={} by {} → new stock={}", productUuid, request.getQuantity(), newStock);
        return "Stock updated to " + newStock;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overwrites the current stock with the quantity from the request
     * and records an ADJUSTMENT movement.</p>
     */
    @Override
    @Transactional
    public String adjustStock(String productUuid, String sellerUuid, StockUpdateRequest request) {
        Product product = findProductBySeller(productUuid, sellerUuid);
        product.setStock(request.getQuantity());
        productRepository.save(product);

        recordMovement(productUuid, StockMovement.MovementType.ADJUSTMENT,
                request.getQuantity(), request.getQuantity(), request.getReference());

        log.info("[INVENTORY] Adjusted product={} stock to {}", productUuid, request.getQuantity());
        return "Stock adjusted to " + request.getQuantity();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a paginated view of {@link StockMovement} records for the
     * given product, ordered by creation date descending.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getStockHistory(String productUuid, int page, int size) {
        Page<StockMovement> movements = stockMovementRepository
                .findByProductUuidOrderByCreatedAtDesc(productUuid, PageRequest.of(page, size));

        List<StockMovementResponse> content = movements.getContent().stream()
                .map(m -> StockMovementResponse.builder()
                        .id(m.getId())
                        .productUuid(m.getProductUuid())
                        .type(m.getType())
                        .quantity(m.getQuantity())
                        .stockAfter(m.getStockAfter())
                        .reference(m.getReference())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.<StockMovementResponse>builder()
                .content(content)
                .page(movements.getNumber())
                .size(movements.getSize())
                .totalElements(movements.getTotalElements())
                .totalPages(movements.getTotalPages())
                .last(movements.isLast())
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Streams the seller's non-deleted products and filters for those
     * whose stock is at or below the given threshold.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getLowStockProducts(String sellerUuid, int threshold) {
        return productRepository.findBySellerUuidAndIsDeletedFalse(sellerUuid).stream()
                .filter(p -> p.getStock() <= threshold)
                .map(Product::getUuid)
                .toList();
    }

    /**
     * Fetches a product by UUID and verifies that the requesting seller owns it.
     *
     * @param productUuid the UUID of the product
     * @param sellerUuid  the UUID of the seller
     * @return the validated {@link Product}
     * @throws ProductNotFoundException if the product does not exist
     * @throws RuntimeException         if the seller is not the product owner
     */
    private Product findProductBySeller(String productUuid, String sellerUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productUuid));
        if (!product.getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner of this product");
        }
        return product;
    }

    /**
     * Persists a {@link StockMovement} audit record.
     *
     * @param productUuid the UUID of the affected product
     * @param type        the movement type (RESTOCK, ADJUSTMENT, etc.)
     * @param quantity    the quantity involved in this movement
     * @param stockAfter  the stock level after the movement
     * @param reference   an optional external reference (e.g. order ID)
     */
    private void recordMovement(String productUuid, StockMovement.MovementType type,
                                int quantity, int stockAfter, String reference) {
        stockMovementRepository.save(StockMovement.builder()
                .productUuid(productUuid)
                .type(type)
                .quantity(quantity)
                .stockAfter(stockAfter)
                .reference(reference)
                .build());
    }
}
