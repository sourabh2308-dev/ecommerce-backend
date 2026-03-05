package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.dto.request.VariantRequest;
import com.sourabh.product_service.dto.response.VariantResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.entity.ProductVariant;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.repository.ProductVariantRepository;
import com.sourabh.product_service.service.VariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link VariantService} for managing product variants.
 *
 * <p>Variants represent different configurations of a product (e.g. size,
 * colour). Each variant may carry its own price override, independent stock
 * level, and SKU code. Variants support soft-delete via an {@code isActive}
 * flag. Duplicate name/value combinations within the same product are
 * rejected.</p>
 *
 * @see VariantService
 * @see ProductVariantRepository
 */
@Service
@RequiredArgsConstructor
public class VariantServiceImpl implements VariantService {

    /** Repository for product entity lookups. */
    private final ProductRepository productRepository;

    /** Repository for product-variant persistence. */
    private final ProductVariantRepository variantRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Rejects the operation if a variant with the same name and value
     * already exists for the product.</p>
     */
    @Override
    @Transactional
    public VariantResponse addVariant(String productUuid, String sellerUuid, VariantRequest request) {
        Product product = getOwnedProduct(productUuid, sellerUuid);

        if (variantRepository.existsByProductIdAndVariantNameAndVariantValue(
                product.getId(), request.getVariantName(), request.getVariantValue())) {
            throw new IllegalArgumentException("Variant already exists");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .variantName(request.getVariantName())
                .variantValue(request.getVariantValue())
                .priceOverride(request.getPriceOverride())
                .stock(request.getStock())
                .sku(request.getSku())
                .build();

        return mapToResponse(variantRepository.save(variant));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<VariantResponse> getVariants(String productUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return variantRepository.findByProductIdAndIsActiveTrue(product.getId())
                .stream().map(this::mapToResponse).toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>All mutable fields (name, value, price override, stock, SKU) are
     * replaced with the values from the request.</p>
     */
    @Override
    @Transactional
    public VariantResponse updateVariant(String variantUuid, String sellerUuid, VariantRequest request) {
        ProductVariant variant = variantRepository.findByUuid(variantUuid)
                .orElseThrow(() -> new ProductNotFoundException("Variant not found"));
        if (!variant.getProduct().getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner");
        }
        variant.setVariantName(request.getVariantName());
        variant.setVariantValue(request.getVariantValue());
        variant.setPriceOverride(request.getPriceOverride());
        variant.setStock(request.getStock());
        variant.setSku(request.getSku());
        return mapToResponse(variantRepository.save(variant));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets the variant's {@code isActive} flag to {@code false} rather
     * than physically removing the record.</p>
     */
    @Override
    @Transactional
    public String deleteVariant(String variantUuid, String sellerUuid) {
        ProductVariant variant = variantRepository.findByUuid(variantUuid)
                .orElseThrow(() -> new ProductNotFoundException("Variant not found"));
        if (!variant.getProduct().getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner");
        }
        variant.setIsActive(false);
        variantRepository.save(variant);
        return "Variant deactivated";
    }

    /**
     * Retrieves a product and verifies seller ownership.
     *
     * @param productUuid the UUID of the product
     * @param sellerUuid  the expected owner's UUID
     * @return the validated {@link Product}
     * @throws ProductNotFoundException if the product does not exist
     * @throws RuntimeException         if the seller is not the owner
     */
    private Product getOwnedProduct(String productUuid, String sellerUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        if (!product.getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner of this product");
        }
        return product;
    }

    /**
     * Maps a {@link ProductVariant} entity to a {@link VariantResponse} DTO.
     *
     * @param v the variant entity
     * @return the response DTO
     */
    private VariantResponse mapToResponse(ProductVariant v) {
        return VariantResponse.builder()
                .uuid(v.getUuid())
                .variantName(v.getVariantName())
                .variantValue(v.getVariantValue())
                .priceOverride(v.getPriceOverride())
                .stock(v.getStock())
                .sku(v.getSku())
                .isActive(v.getIsActive())
                .build();
    }
}
