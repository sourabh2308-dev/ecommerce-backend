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

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    @PostMapping("/{productUuid}/variants")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<VariantResponse> addVariant(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.ok(variantService.addVariant(productUuid, sellerUuid, request));
    }

    @GetMapping("/{productUuid}/variants")
    public ResponseEntity<List<VariantResponse>> getVariants(@PathVariable String productUuid) {
        return ResponseEntity.ok(variantService.getVariants(productUuid));
    }

    @PutMapping("/variants/{variantUuid}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<VariantResponse> updateVariant(
            @PathVariable String variantUuid,
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.ok(variantService.updateVariant(variantUuid, sellerUuid, request));
    }

    @DeleteMapping("/variants/{variantUuid}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> deleteVariant(
            @PathVariable String variantUuid,
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(variantService.deleteVariant(variantUuid, sellerUuid));
    }
}
