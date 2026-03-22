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

@Service
@RequiredArgsConstructor
public class VariantServiceImpl implements VariantService {

    private final ProductRepository productRepository;

    private final ProductVariantRepository variantRepository;

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

    @Override
    @Transactional(readOnly = true)
    public List<VariantResponse> getVariants(String productUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return variantRepository.findByProductIdAndIsActiveTrue(product.getId())
                .stream().map(this::mapToResponse).toList();
    }

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

    private Product getOwnedProduct(String productUuid, String sellerUuid) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        if (!product.getSellerUuid().equals(sellerUuid)) {
            throw new RuntimeException("Not the owner of this product");
        }
        return product;
    }

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
