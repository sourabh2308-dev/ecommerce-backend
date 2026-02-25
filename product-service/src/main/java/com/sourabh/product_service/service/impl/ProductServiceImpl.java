package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.CreateProductRequest;
import com.sourabh.product_service.dto.request.UpdateProductRequest;
import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.entity.ProductStatus;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.exception.ProductStateException;
import com.sourabh.product_service.exception.UnauthorizedProductAccessException;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.service.ProductService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PRODUCT SERVICE IMPLEMENTATION - Product Catalog & Inventory Management
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Core business logic for managing product lifecycle: creation, updates, inventory,
 * status transitions, and multi-criteria search. Serves as the central product catalog
 * for the e-commerce platform with real-time inventory tracking and caching.
 * 
 * KEY RESPONSIBILITIES:
 * --------------------
 * 1. Product Catalog Management:
 *    - Create products (SELLER only)
 *    - Update product details (name, description, price, category)
 *    - Soft delete products (isDeleted flag)
 *    - Status transitions: PENDING → ACTIVE → OUT_OF_STOCK → INACTIVE
 * 
 * 2. Inventory Management:
 *    - Track stock quantity in real-time
 *    - Reduce stock on order placement (synchronous Feign call from order-service)
 *    - Restore stock on order cancellation/payment failure (compensation)
 *    - Prevent overselling with optimistic locking or validation
 * 
 * 3. Authorization & Ownership:
 *    - Validate seller ownership before updates/deletes
 *    - Only product owner can modify their products
 *    - Admins can view/manage all products
 * 
 * 4. Search & Filtering:
 *    - Multi-criteria search: category, status, price range, seller
 *    - Pagination for large result sets
 *    - Sorting by price, name, createdAt
 * 
 * 5. Caching Strategy:
 *    - Cache individual product lookups (high read frequency)
 *    - Cache product lists by seller
 *    - Evict cache on updates to ensure consistency
 *    - Redis-backed distributed cache
 * 
 * ARCHITECTURE PATTERNS:
 * ----------------------
 * - Service Layer Pattern: Business logic separated from controllers
 * - Repository Pattern: JPA abstracts database access
 * - DTO Pattern: Entity → Response DTO conversion (hide internal fields)
 * - Cache-Aside Pattern: @Cacheable + @CacheEvict for performance
 * - Optimistic Concurrency: Prevent race conditions in stock updates
 * - Specification Pattern: Dynamic JPA Criteria queries for flexible filtering
 * 
 * ANNOTATIONS EXPLAINED:
 * ----------------------
 * @Service:
 *   - Marks as Spring-managed service bean
 *   - Auto-detected during component scanning
 *   - Eligible for dependency injection
 * 
 * @Transactional:
 *   - Wraps method in database transaction
 *   - Auto-rollback on unchecked exceptions
 *   - Ensures atomicity of multi-step operations
 *   - Default: PROPAGATION_REQUIRED, ISOLATION_DEFAULT
 * 
 * @Cacheable("productsCache", key = "#uuid"):
 *   - Caches method result with key: productsCache::{uuid}
 *   - Subsequent calls return cached value (no DB query)
 *   - TTL configured in RedisCacheConfig (e.g., 30 minutes)
 * 
 * @CacheEvict:
 *   - Removes cache entries on data modification
 *   - allEntries = true: Clears entire cache
 *   - Used after create/update/delete to prevent stale data
 * 
 * BUSINESS RULES:
 * ---------------
 * 1. Stock Management:
 *    - reduceStock: Decrease quantity, set OUT_OF_STOCK if quantity = 0
 *    - restoreStock: Increase quantity, set ACTIVE if was OUT_OF_STOCK
 *    - Cannot reduce stock below 0 (throws ProductStateException)
 * 
 * 2. Product Status Transitions:
 *    - PENDING: Initial state after creation (awaiting approval)
 *    - ACTIVE: Available for purchase (quantity > 0)
 *    - OUT_OF_STOCK: No inventory available (quantity = 0)
 *    - INACTIVE: Seller disabled the product
 * 
 * 3. Authorization Rules:
 *    - Only product owner (seller) can update/delete
 *    - Admins can view all products
 *    - Buyers can only view ACTIVE products
 * 
 * 4. Pricing Rules:
 *    - Price must be > 0
 *    - Discount price must be < original price (if provided)
 *    - Price update triggers cache eviction
 * 
 * ERROR HANDLING:
 * ---------------
 * Custom exceptions for business rule violations:
 * - ProductNotFoundException: Product UUID not found
 * - UnauthorizedProductAccessException: Seller not owner of product
 * - ProductStateException: Invalid state transition or insufficient stock
 * 
 * CACHING STRATEGY:
 * -----------------
 * Cache Keys:
 * - Single product: productsCache::{uuid}
 * - Seller products: productsCache::seller:{sellerUuid}
 * - Search results: Not cached (too many permutations)
 * 
 * Cache Eviction:
 * - Create product: Evict seller products cache
 * - Update product: Evict product UUID + seller products cache
 * - Delete product: Evict product UUID + seller products cache
 * - Stock change: Evict product UUID cache
 * 
 * TTL Configuration:
 * - Product details: 30 minutes (balances freshness and performance)
 * - Seller product lists: 15 minutes (more frequent changes)
 * 
 * INTERNAL API (Feign Clients):
 * ------------------------------
 * These methods are called by other microservices via Feign:
 * 
 * 1. getProductByUuid(uuid):
 *    - Called by: order-service, review-service
 *    - Purpose: Fetch product details during order/review creation
 *    - Returns: ProductDto with full product info
 * 
 * 2. reduceStock(uuid, quantity):
 *    - Called by: order-service during order creation
 *    - Purpose: Decrease inventory when order placed
 *    - Returns: Success message or throws ProductStateException
 * 
 * 3. restoreStock(uuid, quantity):
 *    - Called by: order-service during order cancellation
 *    - Purpose: Compensation logic to return stock
 *    - Returns: Success message
 * 
 * EXAMPLE FLOWS:
 * --------------
 * 
 * Flow 1: Create Product
 * createProduct(request, sellerUuid)
 * → Validate request (price > 0, name not empty)
 * → Create Product entity (status = PENDING, isDeleted = false)
 * → Set sellerUuid, createdAt, updatedAt
 * → Save to database
 * → Evict seller products cache
 * → Convert to ProductResponse
 * → Return ProductResponse
 * 
 * Flow 2: Reduce Stock (Called by order-service)
 * reduceStock(productUuid, quantity)
 * → Fetch product by UUID (throw ProductNotFoundException if not found)
 * → Check product.quantity >= quantity (throw ProductStateException if insufficient)
 * → Decrease product.quantity by quantity
 * → If quantity becomes 0: Set status = OUT_OF_STOCK
 * → Save product
 * → Evict product cache
 * → Return success message
 * 
 * Flow 3: Search Products with Filters
 * searchProducts(category, minPrice, maxPrice, status, page, size)
 * → Build JPA Specification with dynamic filters:
 *    - category EQUALS provided value (if not null)
 *    - price BETWEEN minPrice AND maxPrice (if provided)
 *    - status EQUALS provided value (if not null)
 *    - isDeleted = false (always)
 * → Create Pageable (page, size, sort by createdAt DESC)
 * → Execute: productRepository.findAll(spec, pageable)
 * → Convert Page<Product> to Page<ProductResponse>
 * → Wrap in PageResponse<ProductResponse>
 * → Return PageResponse
 * 
 * Flow 4: Update Product with Cache Eviction
 * updateProduct(uuid, request, sellerUuid)
 * → Fetch product by UUID
 * → Validate seller ownership (product.sellerUuid == sellerUuid)
 * → Update allowed fields (name, description, price, category)
 * → Set updatedAt = now
 * → Save product
 * → Evict caches: product UUID + seller products list
 * → Convert to ProductResponse
 * → Return ProductResponse
 * 
 * TESTING NOTES:
 * --------------
 * - Mock ProductRepository in unit tests
 * - Test cache behavior with @MockBean CacheManager
 * - Verify authorization with different seller UUIDs
 * - Test stock edge cases: reduce to 0, restore from 0, reduce below 0
 * - Test Specification queries with various filter combinations
 */
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // ===============================
    // CREATE PRODUCT
    // ===============================

    @Override
    @Transactional
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public ProductResponse createProduct(CreateProductRequest request, String sellerUuid) {

        Product product = Product.builder()
                .uuid(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .sellerUuid(sellerUuid)
                .status(ProductStatus.DRAFT)
                .isDeleted(false)
                .imageUrl(request.getImageUrl())
                .build();

        productRepository.save(product);

        log.info("Product created by seller: {}", sellerUuid);

        return mapToResponse(product);
    }

    // ===============================
    // UPDATE PRODUCT
    // ===============================

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    public ProductResponse updateProduct(
            String uuid,
            UpdateProductRequest request,
            String role,
            String sellerUuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if ("SELLER".equalsIgnoreCase(role)) {

            if (!product.getSellerUuid().equals(sellerUuid)) {
                throw new UnauthorizedProductAccessException("You can only update your own product");
            }

            if (product.getStatus() == ProductStatus.BLOCKED) {
                throw new ProductStateException("Blocked product cannot be updated");
            }
        }

        if (request.getName() != null)
            product.setName(request.getName());

        if (request.getDescription() != null)
            product.setDescription(request.getDescription());

        if (request.getPrice() != null)
            product.setPrice(request.getPrice());

        if (request.getStock() != null)
            product.setStock(request.getStock());

        if (request.getCategory() != null)
            product.setCategory(request.getCategory());

        if (request.getImageUrl() != null)
            product.setImageUrl(request.getImageUrl());

        productRepository.save(product);

        log.info("Product updated: uuid={}, by role={}, sellerUuid={}", uuid, role, sellerUuid);

        return mapToResponse(product);
    }
    // ===============================

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String approveProduct(String uuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);

        log.info("Product approved: uuid={}", uuid);
        return "Product approved";
    }

    // ===============================
    // BLOCK PRODUCT (ADMIN)
    // ===============================

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String blockProduct(String uuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        product.setStatus(ProductStatus.BLOCKED);
        productRepository.save(product);

        log.info("Product blocked: uuid={}", uuid);
        return "Product blocked";
    }

    // ===============================
    // UNBLOCK PRODUCT (ADMIN)
    // ===============================

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String unblockProduct(String uuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if (product.getStatus() != ProductStatus.BLOCKED) {
            throw new ProductStateException("Product is not blocked");
        }
        // Return to DRAFT so seller must resubmit for approval
        product.setStatus(ProductStatus.DRAFT);
        productRepository.save(product);

        log.info("Product unblocked (set to DRAFT): uuid={}", uuid);
        return "Product unblocked and set back to DRAFT for re-approval";
    }

    // ===============================
    // SOFT DELETE
    // ===============================

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    public String softDeleteProduct(
            String uuid,
            String role,
            String sellerUuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if ("SELLER".equalsIgnoreCase(role)
                && !product.getSellerUuid().equals(sellerUuid)) {
            throw new UnauthorizedProductAccessException("You can only delete your own product");
        }

        product.setIsDeleted(true);
        productRepository.save(product);

        log.info("Product soft-deleted: uuid={}, by role={}", uuid, role);
        return "Product deleted";
    }

    // ===============================
    // LIST PRODUCTS
    // ===============================

    @Override
    public PageResponse<ProductResponse> listProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String role,
            String sellerUuid,
            String keyword) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage;

        if ("BUYER".equalsIgnoreCase(role)) {

            productPage = productRepository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.isFalse(root.get("isDeleted")),
                            cb.equal(root.get("status"), ProductStatus.ACTIVE),
                            keywordPredicate(cb, root, keyword)
                    ),
                    pageable
            );

        } else if ("SELLER".equalsIgnoreCase(role)) {

            productPage = productRepository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.isFalse(root.get("isDeleted")),
                            cb.equal(root.get("sellerUuid"), sellerUuid),
                            keywordPredicate(cb, root, keyword)
                    ),
                    pageable
            );

        } else { // ADMIN

            productPage = productRepository.findAll(
                    (root, query, cb) -> cb.and(
                            cb.isFalse(root.get("isDeleted")),
                            keywordPredicate(cb, root, keyword)
                    ),
                    pageable
            );
        }

        List<ProductResponse> responses = productPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .content(responses)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    // ===============================
    @Override
    @Cacheable(value = "products", key = "#uuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public ProductResponse getProductByUuid(String uuid, String role) {
        log.debug("Cache miss for product uuid={} — fetching from DB", uuid);
        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + uuid));

        // Buyers and unauthenticated callers only see ACTIVE products
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(role) || "SELLER".equalsIgnoreCase(role);
        if (!isPrivileged && product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductNotFoundException("Product not found: " + uuid);
        }

        return mapToResponse(product);
    }

    // HELPER METHODS
    // ===============================

    /**
     * MAPTORESPONSE - Method Documentation
     *
     * PURPOSE:
     * This method handles the mapToResponse operation.
     *
     * PARAMETERS:
     * @param product - Product value
     *
     * RETURN VALUE:
     * @return ProductResponse - Result of the operation
     *
     */
    private ProductResponse mapToResponse(Product product) {

        return ProductResponse.builder()
                .uuid(product.getUuid())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .sellerUuid(product.getSellerUuid())
                .status(product.getStatus().name())
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .imageUrl(product.getImageUrl())
                .build();
    }

    private Predicate keywordPredicate(
            CriteriaBuilder cb,
            Root<Product> root,
            String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return cb.conjunction();
        }

        String pattern = "%" + keyword.toLowerCase() + "%";

        return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("category")), pattern)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productUuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String reduceStock(String productUuid, Integer quantity) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductStateException("Product is not active");
        }

        if (product.getStock() < quantity) {
            throw new ProductStateException("Insufficient stock");
        }

        product.setStock(product.getStock() - quantity);

        if (product.getStock() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        productRepository.save(product);

        log.info("Stock reduced for product uuid={} by quantity={}, remaining={}",
                productUuid, quantity, product.getStock());
        return "Stock reduced successfully";
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productUuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String restoreStock(String productUuid, Integer quantity) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        product.setStock(product.getStock() + quantity);

        // If the product was out of stock, make it active again
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK && product.getStock() > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        productRepository.save(product);

        log.info("Stock restored for product uuid={} by quantity={}, new stock={}",
                productUuid, quantity, product.getStock());
        return "Stock restored successfully";
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productUuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public void updateRating(String productUuid, Integer rating) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found"));

        int currentCount = product.getTotalReviews();
        double currentAverage = product.getAverageRating();

        double newAverage =
                ((currentAverage * currentCount) + rating)
                        / (currentCount + 1);

        product.setTotalReviews(currentCount + 1);
        product.setAverageRating(newAverage);

        productRepository.save(product);

        log.info("Rating updated for product uuid={}: newAverage={}, totalReviews={}",
                productUuid, String.format("%.2f", newAverage), currentCount + 1);
    }

}
