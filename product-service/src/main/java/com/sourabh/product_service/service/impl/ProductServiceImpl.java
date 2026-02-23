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
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // ===============================
    // CREATE PRODUCT
    // ===============================

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

        productRepository.save(product);

        log.info("Product updated: uuid={}, by role={}, sellerUuid={}", uuid, role, sellerUuid);

        return mapToResponse(product);
    }
    // ===============================

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

    // ===============================
    // BLOCK PRODUCT (ADMIN)
    // ===============================

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
    public ProductResponse getProductByUuid(String uuid) {
        log.debug("Cache miss for product uuid={} — fetching from DB", uuid);
        Product product = productRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + uuid));
        return mapToResponse(product);
    }

    // HELPER METHODS
    // ===============================

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
