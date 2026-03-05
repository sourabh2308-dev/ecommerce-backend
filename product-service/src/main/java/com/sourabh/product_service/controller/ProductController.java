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

/**
 * Primary REST controller for product lifecycle operations.
 * <p>
 * Exposes endpoints for creating, updating, approving, blocking, deleting,
 * listing, and searching products. Seller-specific actions require the
 * {@code SELLER} role, administrative actions require {@code ADMIN}, and
 * public read endpoints are unauthenticated. Internal service-to-service
 * endpoints (stock reduction, restoration, rating updates) are secured
 * via an {@code X-Internal-Secret} header validated by
 * {@code InternalSecretFilter}.
 * </p>
 *
 * <p>Base path: {@code /api/product}</p>
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    /** Service encapsulating core product business logic. */
    private final ProductService productService;

    /** Service providing Elasticsearch-backed product search and indexing. */
    private final ProductSearchService productSearchService;

    /**
     * Creates a new product on behalf of the authenticated seller.
     * The product is initially set to {@code DRAFT} status, pending admin approval.
     *
     * @param request     validated product creation payload
     * @param httpRequest servlet request carrying the {@code X-User-UUID} header
     * @return the newly created product
     */
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

    /**
     * Updates an existing product. Sellers may only update their own products;
     * admins may update any product.
     *
     * @param uuid        UUID of the product to update
     * @param request     validated update payload
     * @param httpRequest servlet request carrying role and seller UUID headers
     * @return the updated product
     */
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

    /**
     * Approves a product, transitioning it from {@code DRAFT} to {@code ACTIVE}.
     * Only accessible to administrators.
     *
     * @param uuid UUID of the product to approve
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/approve/{uuid}")
    public ResponseEntity<String> approveProduct(@PathVariable String uuid) {
        return ResponseEntity.ok(productService.approveProduct(uuid));
    }

    /**
     * Blocks a product, hiding it from public listings.
     * Only accessible to administrators.
     *
     * @param uuid UUID of the product to block
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/block/{uuid}")
    public ResponseEntity<String> blockProduct(@PathVariable String uuid) {
        return ResponseEntity.ok(productService.blockProduct(uuid));
    }

    /**
     * Unblocks a previously blocked product, making it visible again.
     * Only accessible to administrators.
     *
     * @param uuid UUID of the product to unblock
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/unblock/{uuid}")
    public ResponseEntity<String> unblockProduct(@PathVariable String uuid) {
        return ResponseEntity.ok(productService.unblockProduct(uuid));
    }

    /**
     * Soft-deletes a product by setting its {@code isDeleted} flag to {@code true}.
     * Sellers may only delete their own products; admins may delete any product.
     *
     * @param uuid        UUID of the product to delete
     * @param httpRequest servlet request carrying role and seller UUID headers
     * @return confirmation message
     */
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

    /**
     * Lists products with offset-based pagination, sorting, and optional keyword filtering.
     * <p>
     * Visibility rules depend on the caller's role: admins see all products,
     * sellers see their own, and anonymous/buyer users see only active products.
     * </p>
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 10)
     * @param sortBy      field to sort by (default {@code createdAt})
     * @param direction   sort direction — {@code asc} or {@code desc} (default {@code desc})
     * @param keyword     optional search keyword for name/description filtering
     * @param httpRequest servlet request carrying role and seller UUID headers
     * @return paginated list of products
     */
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

    /**
     * Retrieves a single product by its UUID. Visibility depends on the caller's role.
     *
     * @param uuid        UUID of the product
     * @param httpRequest servlet request carrying the role header
     * @return the matching product
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role = httpRequest.getHeader("X-User-Role");
        return ResponseEntity.ok(productService.getProductByUuid(uuid, role));
    }

    /**
     * Internal endpoint: reduces stock for a product after an order is placed.
     * Secured via {@code X-Internal-Secret} header (no JWT required).
     *
     * @param uuid     UUID of the product
     * @param quantity number of units to deduct
     * @return confirmation message
     */
    @PutMapping("/internal/reduce-stock/{uuid}")
    public ResponseEntity<String> reduceStock(
            @PathVariable String uuid,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                productService.reduceStock(uuid, quantity)
        );
    }

    /**
     * Internal endpoint: restores stock for a product after an order cancellation
     * or payment failure. Secured via {@code X-Internal-Secret} header.
     *
     * @param uuid     UUID of the product
     * @param quantity number of units to restore
     * @return confirmation message
     */
    @PutMapping("/internal/restore-stock/{uuid}")
    public ResponseEntity<String> restoreStock(
            @PathVariable String uuid,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                productService.restoreStock(uuid, quantity)
        );
    }

    /**
     * Internal endpoint: updates the average rating for a product after a new
     * review is submitted. Secured via {@code X-Internal-Secret} header.
     *
     * @param uuid   UUID of the product
     * @param rating the new review rating value
     * @return HTTP 200 with empty body
     */
    @PutMapping("/internal/update-rating/{uuid}")
    public ResponseEntity<Void> updateRating(
            @PathVariable String uuid,
            @RequestParam Integer rating) {

        productService.updateRating(uuid, rating);
        return ResponseEntity.ok().build();
    }

    /**
     * Lists products using cursor-based (keyset) pagination.
     * More efficient than offset pagination for large datasets, as it avoids
     * counting all preceding rows.
     *
     * @param cursor database ID to start after ({@code null} for the first page)
     * @param size   page size (default 20)
     * @return cursor-paginated product list with a {@code nextCursor} pointer
     */
    @GetMapping("/cursor")
    public ResponseEntity<CursorPageResponse<ProductResponse>> listProductsCursor(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.listProductsCursor(cursor, size));
    }

    /**
     * Full-text product search powered by Elasticsearch.
     * Supports filtering by category and price range, and returns results
     * sorted by average rating (descending) then price (ascending).
     *
     * @param q        optional free-text query matched against name, description, and category
     * @param category optional category filter (exact match)
     * @param minPrice optional minimum price filter (inclusive)
     * @param maxPrice optional maximum price filter (inclusive)
     * @param size     maximum number of results (default 20, max 100)
     * @return list of matching products
     */
    @GetMapping("/search")
    public ResponseEntity<java.util.List<ProductResponse>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(productSearchService.search(q, category, minPrice, maxPrice, size));
    }

    /**
     * Returns product name suggestions matching the given prefix.
     * Used for search-as-you-type autocomplete in the frontend.
     *
     * @param prefix the search prefix to match against product names
     * @param size   maximum number of suggestions (default 10, max 50)
     * @return list of matching product names
     */
    @GetMapping("/search/autocomplete")
    public ResponseEntity<java.util.List<String>> autocompleteProducts(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(productSearchService.autocomplete(prefix, size));
    }

    /**
     * Triggers a full reindex of all products into Elasticsearch.
     * Only accessible to administrators.
     *
     * @return confirmation message
     */
    @PostMapping("/search/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reindexProducts() {
        productSearchService.indexAllProducts();
        return ResponseEntity.ok("Reindex started/completed successfully");
    }

}
