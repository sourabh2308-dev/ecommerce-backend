package com.sourabh.product_service.controller;

import com.sourabh.product_service.dto.request.VariantRequest;
import com.sourabh.product_service.dto.response.VariantResponse;
import com.sourabh.product_service.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing product variants (e.g. size, colour, material).
 * <p>
 * All mutating operations are restricted to the {@code SELLER} role, while
 * variant listing is publicly available.
 * </p>
 *
 * <p>Base path: {@code /api/product}</p>
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class VariantController {

    /** Service encapsulating variant business logic. */
    private final VariantService variantService;

    /**
     * Adds a new variant to a product.
     *
     * @param productUuid UUID of the target product
     * @param sellerUuid  UUID of the authenticated seller
     * @param request     validated payload containing variant details
     * @return the newly created variant
     */
    @PostMapping("/{productUuid}/variants")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<VariantResponse> addVariant(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.ok(variantService.addVariant(productUuid, sellerUuid, request));
    }

    /**
     * Lists all active variants for a product.
     *
     * @param productUuid UUID of the product
     * @return list of active variant responses
     */
    @GetMapping("/{productUuid}/variants")
    public ResponseEntity<List<VariantResponse>> getVariants(@PathVariable String productUuid) {
        return ResponseEntity.ok(variantService.getVariants(productUuid));
    }

    /**
     * Updates an existing variant.
     *
     * @param variantUuid UUID of the variant to update
     * @param sellerUuid  UUID of the authenticated seller
     * @param request     validated payload with updated variant details
     * @return the updated variant
     */
    @PutMapping("/variants/{variantUuid}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<VariantResponse> updateVariant(
            @PathVariable String variantUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.ok(variantService.updateVariant(variantUuid, sellerUuid, request));
    }

    /**
     * Soft-deletes a variant by marking it as inactive.
     *
     * @param variantUuid UUID of the variant to delete
     * @param sellerUuid  UUID of the authenticated seller
     * @return confirmation message
     */
    @DeleteMapping("/variants/{variantUuid}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> deleteVariant(
            @PathVariable String variantUuid,
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(variantService.deleteVariant(variantUuid, sellerUuid));
    }
}
