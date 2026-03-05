package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.VariantRequest;
import com.sourabh.product_service.dto.response.VariantResponse;

import java.util.List;

/**
 * Service interface for managing product variants (e.g., size, colour).
 *
 * <p>Each variant belongs to a product and carries its own optional price
 * override, independent stock level, and SKU. Variants support soft-delete
 * (deactivation) semantics.</p>
 *
 * @see com.sourabh.product_service.service.impl.VariantServiceImpl
 * @see com.sourabh.product_service.entity.ProductVariant
 */
public interface VariantService {

    /**
     * Adds a new variant to a product.
     *
     * @param productUuid the UUID of the parent product
     * @param sellerUuid  the UUID of the seller (must own the product)
     * @param request     variant details (name, value, price override, stock, SKU)
     * @return the created {@link VariantResponse}
     * @throws IllegalArgumentException if the variant name/value combination
     *                                  already exists for this product
     */
    VariantResponse addVariant(String productUuid, String sellerUuid, VariantRequest request);

    /**
     * Returns all active variants for a product.
     *
     * @param productUuid the UUID of the product
     * @return list of active {@link VariantResponse} entries
     */
    List<VariantResponse> getVariants(String productUuid);

    /**
     * Updates an existing variant's details.
     *
     * @param variantUuid the UUID of the variant to update
     * @param sellerUuid  the UUID of the seller (must own the parent product)
     * @param request     new variant values
     * @return the updated {@link VariantResponse}
     */
    VariantResponse updateVariant(String variantUuid, String sellerUuid, VariantRequest request);

    /**
     * Soft-deletes (deactivates) a variant.
     *
     * @param variantUuid the UUID of the variant
     * @param sellerUuid  the UUID of the seller (must own the parent product)
     * @return confirmation message
     */
    String deleteVariant(String variantUuid, String sellerUuid);
}
