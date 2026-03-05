package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.common.PageResponse;
import com.sourabh.product_service.dto.request.CreateProductRequest;
import com.sourabh.product_service.dto.request.UpdateProductRequest;
import com.sourabh.product_service.dto.response.CursorPageResponse;
import com.sourabh.product_service.dto.response.CategoryResponse;
import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.entity.ProductStatus;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.exception.ProductStateException;
import com.sourabh.product_service.exception.UnauthorizedProductAccessException;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.search.service.ProductSearchService;
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

/**
 * Implementation of {@link ProductService} — the central business-logic layer
 * for the product catalog in the e-commerce platform.
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *   <li>Full product lifecycle: create (DRAFT), approve (ACTIVE), block, unblock, soft-delete</li>
 *   <li>Inventory management: reduce / restore stock with automatic status transitions</li>
 *   <li>Role-aware listing and search (BUYER, SELLER, ADMIN)</li>
 *   <li>Rating aggregation triggered by Kafka review events</li>
 *   <li>Redis-backed caching with eviction on mutation</li>
 *   <li>Elasticsearch indexing via {@link ProductSearchService}</li>
 * </ul>
 *
 * <p>Stock mutations ({@code reduceStock}, {@code restoreStock}) are invoked
 * synchronously by order-service through Feign and participate in the
 * saga-based order/payment workflow.</p>
 *
 * @see ProductService
 * @see ProductRepository
 * @see ProductSearchService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    /** JPA repository for product persistence and specification-based queries. */
    private final ProductRepository productRepository;

    /** Elasticsearch indexing service for full-text product search. */
    private final ProductSearchService productSearchService;

    /**
     * {@inheritDoc}
     *
     * <p>Builds a new {@link Product} entity in DRAFT status, persists it,
     * and indexes it in Elasticsearch for full-text search.</p>
     */
    @Override
    @Transactional
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
        productSearchService.indexProductByUuid(product.getUuid());

        log.info("Product created by seller: {}", sellerUuid);

        return mapToResponse(product);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sellers may only update their own non-blocked products. Null fields
     * in the request are ignored (partial update). Stock changes trigger
     * automatic status transitions between ACTIVE and OUT_OF_STOCK.</p>
     */
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

        if (request.getStock() != null) {
            product.setStock(request.getStock());

            if (request.getStock() <= 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                product.setStatus(ProductStatus.ACTIVE);
            }
        }

        if (request.getCategory() != null)
            product.setCategory(request.getCategory());

        if (request.getImageUrl() != null)
            product.setImageUrl(request.getImageUrl());

        productRepository.save(product);
        productSearchService.indexProductByUuid(product.getUuid());

        log.info("Product updated: uuid={}, by role={}, sellerUuid={}", uuid, role, sellerUuid);

        return mapToResponse(product);
    }
    /**
     * {@inheritDoc}
     *
     * <p>Transitions the product from DRAFT to ACTIVE so it becomes
     * visible to buyers.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    public String approveProduct(String uuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);

        log.info("Product approved: uuid={}", uuid);
        return "Product approved";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets the product status to BLOCKED, preventing purchase.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    public String blockProduct(String uuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        product.setStatus(ProductStatus.BLOCKED);
        productRepository.save(product);

        log.info("Product blocked: uuid={}", uuid);
        return "Product blocked";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only applicable when the product is currently BLOCKED. Resets
     * the status to DRAFT so the seller must resubmit for approval.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#uuid")
    public String unblockProduct(String uuid) {

        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if (product.getStatus() != ProductStatus.BLOCKED) {
            throw new ProductStateException("Product is not blocked");
        }
        product.setStatus(ProductStatus.DRAFT);
        productRepository.save(product);

        log.info("Product unblocked (set to DRAFT): uuid={}", uuid);
        return "Product unblocked and set back to DRAFT for re-approval";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the product as deleted and removes it from the
     * Elasticsearch index. Sellers may only delete their own products.</p>
     */
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
        productSearchService.removeProductFromIndex(product.getUuid());

        log.info("Product soft-deleted: uuid={}, by role={}", uuid, role);
        return "Product deleted";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds JPA Specification predicates dynamically based on the
     * caller's role and an optional keyword filter that matches against
     * name, description, and category.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Result is cached in Redis under the {@code products} cache region.
     * Buyers and unauthenticated callers only receive ACTIVE products.</p>
     */
    @Override
    @Cacheable(value = "products", key = "#uuid")
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

    /**
     * Converts a {@link Product} entity into a {@link ProductResponse} DTO,
     * including an optional nested category reference.
     *
     * @param product the product entity
     * @return the populated response DTO
     */
    private ProductResponse mapToResponse(Product product) {
        CategoryResponse categoryRef = null;
        if (product.getCategoryRef() != null) {
            categoryRef = mapCategoryToResponse(product.getCategoryRef());
        }

        return ProductResponse.builder()
                .uuid(product.getUuid())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .categoryRef(categoryRef)
                .sellerUuid(product.getSellerUuid())
                .status(product.getStatus().name())
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .imageUrl(product.getImageUrl())
                .build();
    }

    /**
     * Maps a {@link com.sourabh.product_service.entity.Category} entity to
     * a {@link CategoryResponse} DTO for embedding in product responses.
     *
     * @param category the category entity
     * @return the category response DTO
     */
    private CategoryResponse mapCategoryToResponse(com.sourabh.product_service.entity.Category category) {
        return CategoryResponse.builder()
                .uuid(category.getUuid())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentUuid(category.getParent() != null ? category.getParent().getUuid() : null)
                .displayOrder(category.getDisplayOrder())
                .isActive(category.isActive())
                .createdAt(category.getCreatedAt())
                .build();
    }

    /**
     * Builds a JPA {@link Predicate} for keyword-based filtering.
     *
     * <p>If the keyword is blank or null a tautology ({@code conjunction})
     * is returned. Otherwise a case-insensitive LIKE is applied across
     * the name, description, and category columns.</p>
     *
     * @param cb      the criteria builder
     * @param root    the product root
     * @param keyword the search keyword (may be null)
     * @return the composed predicate
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Validates the product is ACTIVE and has sufficient stock. If stock
     * reaches zero the status is automatically set to OUT_OF_STOCK.
     * Evicts the product from the Redis cache.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productUuid")
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

    /**
     * {@inheritDoc}
     *
     * <p>Saga compensation method. Restores the given quantity and flips
     * the product back to ACTIVE if it was previously OUT_OF_STOCK.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productUuid")
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

    /**
     * {@inheritDoc}
     *
     * <p>Computes a running average:
     * {@code newAvg = (currentAvg * count + rating) / (count + 1)}.
     * Evicts the product from the cache so subsequent reads reflect the
     * updated rating.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#productUuid")
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

    /**
     * {@inheritDoc}
     *
     * <p>Fetches {@code size + 1} rows to determine whether a next page
     * exists. The extra row is discarded from the result; its ID becomes
     * the {@code nextCursor} for the subsequent page.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductResponse> listProductsCursor(Long cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1); // fetch one extra to check hasNext
        List<Product> products = productRepository.findWithCursor(cursor, pageable);

        boolean hasNext = products.size() > size;
        if (hasNext) {
            products = products.subList(0, size);
        }

        String nextCursor = hasNext ? String.valueOf(products.get(products.size() - 1).getId()) : null;
        long total = productRepository.countByIsDeletedFalse();

        List<ProductResponse> content = products.stream().map(this::mapToResponse).toList();

        return CursorPageResponse.<ProductResponse>builder()
                .content(content)
                .size(content.size())
                .totalElements(total)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

}
