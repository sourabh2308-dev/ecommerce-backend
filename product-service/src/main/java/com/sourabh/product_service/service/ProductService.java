package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.CreateProductRequest;
import com.sourabh.product_service.dto.request.UpdateProductRequest;
import com.sourabh.product_service.dto.response.CursorPageResponse;
import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.common.PageResponse;

/**
 * Core service interface for the product catalog lifecycle.
 *
 * <p>Covers product creation, updates, status transitions (approve / block /
 * unblock / soft-delete), inventory stock operations, rating aggregation,
 * and both offset-based and cursor-based paginated listing. Role-based
 * visibility rules restrict what each caller (BUYER, SELLER, ADMIN) can see.</p>
 *
 * <p>Stock mutation methods ({@link #reduceStock}, {@link #restoreStock}) are
 * invoked synchronously by the order-service via Feign and participate in
 * the saga-based order workflow.</p>
 *
 * @see com.sourabh.product_service.service.impl.ProductServiceImpl
 * @see com.sourabh.product_service.entity.Product
 */
public interface ProductService {

    /**
     * Creates a new product in DRAFT status, owned by the given seller.
     *
     * @param request    product details (name, description, price, stock, category, image)
     * @param sellerUuid the UUID of the creating seller
     * @return the created {@link ProductResponse}
     */
    ProductResponse createProduct(CreateProductRequest request, String sellerUuid);

    /**
     * Updates an existing product.
     *
     * <p>Sellers may only update their own products and cannot update
     * blocked products. Admins may update any product.</p>
     *
     * @param uuid       the product UUID
     * @param request    fields to update (nulls are ignored)
     * @param role       caller role (SELLER / ADMIN)
     * @param sellerUuid the seller's UUID (used for ownership validation)
     * @return the updated {@link ProductResponse}
     */
    ProductResponse updateProduct(String uuid,
                                  UpdateProductRequest request,
                                  String role,
                                  String sellerUuid);

    /**
     * Approves a product (ADMIN only), transitioning it to ACTIVE status.
     *
     * @param uuid the product UUID
     * @return confirmation message
     */
    String approveProduct(String uuid);

    /**
     * Blocks a product (ADMIN only), preventing it from being purchased.
     *
     * @param uuid the product UUID
     * @return confirmation message
     */
    String blockProduct(String uuid);

    /**
     * Unblocks a previously blocked product, returning it to DRAFT
     * so the seller must resubmit for approval.
     *
     * @param uuid the product UUID
     * @return confirmation message
     */
    String unblockProduct(String uuid);

    /**
     * Soft-deletes a product by setting its {@code isDeleted} flag.
     *
     * <p>Sellers may only delete their own products; admins may delete any.</p>
     *
     * @param uuid       the product UUID
     * @param role       caller role
     * @param sellerUuid the seller's UUID for ownership checks
     * @return confirmation message
     */
    String softDeleteProduct(String uuid, String role, String sellerUuid);

    /**
     * Lists products with offset-based pagination, sorting, and keyword search.
     *
     * <p>Visibility rules per role:</p>
     * <ul>
     *   <li>BUYER &ndash; only ACTIVE, non-deleted products</li>
     *   <li>SELLER &ndash; own non-deleted products (any status)</li>
     *   <li>ADMIN &ndash; all non-deleted products</li>
     * </ul>
     *
     * @param page       zero-based page index
     * @param size       page size
     * @param sortBy     field to sort by (e.g. price, name, createdAt)
     * @param direction  sort direction (asc / desc)
     * @param role       caller role
     * @param sellerUuid seller UUID (required when role is SELLER)
     * @param keyword    optional search keyword matched against name, description, category
     * @return paginated {@link ProductResponse} list
     */
    PageResponse<ProductResponse> listProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String role,
            String sellerUuid,
            String keyword);

    /**
     * Returns a single product by UUID with role-based visibility.
     *
     * <p>Buyers and unauthenticated callers only see ACTIVE products.
     * Sellers and admins see any non-deleted product.</p>
     *
     * @param uuid the product UUID
     * @param role the caller's role (may be {@code null} for unauthenticated)
     * @return the {@link ProductResponse}
     * @throws com.sourabh.product_service.exception.ProductNotFoundException
     *         if the product is not found or not visible to the caller
     */
    ProductResponse getProductByUuid(String uuid, String role);

    /**
     * Reduces stock for a product. Called by order-service during order placement.
     *
     * <p>If stock reaches zero the product status is set to OUT_OF_STOCK.</p>
     *
     * @param productUuid the product UUID
     * @param quantity    the number of units to deduct
     * @return confirmation message
     * @throws com.sourabh.product_service.exception.ProductStateException
     *         if stock is insufficient or the product is not ACTIVE
     */
    String reduceStock(String productUuid, Integer quantity);

    /**
     * Restores stock after a failed payment (saga compensation).
     *
     * <p>If the product was OUT_OF_STOCK and stock becomes positive,
     * the status is flipped back to ACTIVE.</p>
     *
     * @param productUuid the product UUID
     * @param quantity    the number of units to restore
     * @return confirmation message
     */
    String restoreStock(String productUuid, Integer quantity);

    /**
     * Recalculates the product's average rating after a new review.
     *
     * <p>Uses a running-average formula:
     * {@code newAvg = (currentAvg * count + rating) / (count + 1)}.</p>
     *
     * @param productUuid the product UUID
     * @param rating      the new individual rating value
     */
    void updateRating(String productUuid, Integer rating);

    /**
     * Lists products using cursor-based (keyset) pagination.
     *
     * <p>Returns {@code size} products whose ID is greater than the cursor,
     * along with a {@code nextCursor} token for the subsequent page.</p>
     *
     * @param cursor the last-seen product ID (null for the first page)
     * @param size   number of products per page
     * @return cursor-paginated {@link CursorPageResponse}
     */
    CursorPageResponse<ProductResponse> listProductsCursor(Long cursor, int size);
}
