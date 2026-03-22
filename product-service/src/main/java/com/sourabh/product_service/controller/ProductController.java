package com.sourabh.product_service.controller;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.CreateProductRequest;
import com.sourabh.product_service.dto.request.UpdateProductRequest;
import com.sourabh.product_service.dto.response.CursorPageResponse;
import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.search.service.ProductSearchService;
import com.sourabh.product_service.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    private final ProductSearchService productSearchService;

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            HttpServletRequest httpRequest) {

        String sellerUuid = httpRequest.getHeader("X-User-UUID");

        ProductResponse response =
                productService.createProduct(request, sellerUuid);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PutMapping("/{uuid}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateProductRequest request,
            HttpServletRequest httpRequest) {

        String role = httpRequest.getHeader("X-User-Role");
        String sellerUuid = httpRequest.getHeader("X-User-UUID");

        ProductResponse response =
                productService.updateProduct(uuid, request, role, sellerUuid);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/approve/{uuid}")
    public ResponseEntity<String> approveProduct(@PathVariable String uuid) {
        return ResponseEntity.ok(productService.approveProduct(uuid));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/block/{uuid}")
    public ResponseEntity<String> blockProduct(@PathVariable String uuid) {
        return ResponseEntity.ok(productService.blockProduct(uuid));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/unblock/{uuid}")
    public ResponseEntity<String> unblockProduct(@PathVariable String uuid) {
        return ResponseEntity.ok(productService.unblockProduct(uuid));
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {

        String role = httpRequest.getHeader("X-User-Role");
        String sellerUuid = httpRequest.getHeader("X-User-UUID");

        return ResponseEntity.ok(
                productService.softDeleteProduct(uuid, role, sellerUuid)
        );
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest) {

        String role = httpRequest.getHeader("X-User-Role");
        String sellerUuid = httpRequest.getHeader("X-User-UUID");

        PageResponse<ProductResponse> response =
                productService.listProducts(
                        page,
                        size,
                        sortBy,
                        direction,
                        role,
                        sellerUuid,
                        keyword
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role = httpRequest.getHeader("X-User-Role");
        return ResponseEntity.ok(productService.getProductByUuid(uuid, role));
    }

    @PutMapping("/internal/reduce-stock/{uuid}")
    public ResponseEntity<String> reduceStock(
            @PathVariable String uuid,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                productService.reduceStock(uuid, quantity)
        );
    }

    @PutMapping("/internal/restore-stock/{uuid}")
    public ResponseEntity<String> restoreStock(
            @PathVariable String uuid,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                productService.restoreStock(uuid, quantity)
        );
    }

    @PutMapping("/internal/update-rating/{uuid}")
    public ResponseEntity<Void> updateRating(
            @PathVariable String uuid,
            @RequestParam Integer rating) {

        productService.updateRating(uuid, rating);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cursor")
    public ResponseEntity<CursorPageResponse<ProductResponse>> listProductsCursor(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.listProductsCursor(cursor, size));
    }

    @GetMapping("/search")
    public ResponseEntity<java.util.List<ProductResponse>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(productSearchService.search(q, category, minPrice, maxPrice, size));
    }

    @GetMapping("/search/autocomplete")
    public ResponseEntity<java.util.List<String>> autocompleteProducts(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(productSearchService.autocomplete(prefix, size));
    }

    @PostMapping("/search/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reindexProducts() {
        productSearchService.indexAllProducts();
        return ResponseEntity.ok("Reindex started/completed successfully");
    }

}
