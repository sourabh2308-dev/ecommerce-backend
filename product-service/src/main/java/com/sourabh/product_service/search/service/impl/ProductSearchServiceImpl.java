package com.sourabh.product_service.search.service.impl;

import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.search.document.ProductDocument;
import com.sourabh.product_service.search.repository.ProductSearchRepository;
import com.sourabh.product_service.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ProductSearchService}.
 * <p>
 * Bridges the PostgreSQL product store and the Elasticsearch search index.
 * Indexing methods read from the JPA repository and write to the
 * {@link ProductSearchRepository}. Search and autocomplete methods query
 * Elasticsearch, apply in-memory filters (category, price range), and
 * convert results to {@link ProductResponse} DTOs.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {

    /** JPA repository for reading product data from PostgreSQL. */
    private final ProductRepository productRepository;

    /** Elasticsearch repository for indexing and querying product documents. */
    private final ProductSearchRepository productSearchRepository;

    /**
     * {@inheritDoc}
     * <p>
     * Loads all non-deleted products from PostgreSQL, converts them to
     * {@link ProductDocument} instances, and bulk-saves them into the
     * Elasticsearch index.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public void indexAllProducts() {
        List<Product> products = productRepository.findAll()
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .toList();

        List<ProductDocument> docs = products.stream().map(this::toDocument).toList();
        productSearchRepository.saveAll(docs);
        log.info("Indexed {} products into Elasticsearch", docs.size());
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the product exists and is not deleted, it is indexed; otherwise
     * any stale document with that UUID is removed from the index.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public void indexProductByUuid(String productUuid) {
        productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .ifPresentOrElse(product -> {
                    productSearchRepository.save(toDocument(product));
                    log.info("Indexed product {}", productUuid);
                }, () -> {
                    productSearchRepository.deleteById(productUuid);
                    log.info("Removed missing/deleted product {} from index", productUuid);
                });
    }

    /** {@inheritDoc} */
    @Override
    public void removeProductFromIndex(String productUuid) {
        productSearchRepository.deleteById(productUuid);
        log.info("Removed product {} from Elasticsearch index", productUuid);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs an in-memory scan of all indexed documents, applying text
     * matching, category filtering, and price-range constraints. Results
     * are sorted by average rating (descending) then price (ascending)
     * and limited to the requested page size (max 100).
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> search(String query, String category, Double minPrice, Double maxPrice, Integer size) {
        int limit = size == null || size <= 0 ? 20 : Math.min(size, 100);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<ProductDocument> all = StreamSupport.stream(productSearchRepository.findAll().spliterator(), false)
                .filter(doc -> !Boolean.TRUE.equals(doc.getIsDeleted()))
                .filter(doc -> normalizedQuery.isBlank() || containsIgnoreCase(doc.getName(), normalizedQuery)
                        || containsIgnoreCase(doc.getDescription(), normalizedQuery)
                        || containsIgnoreCase(doc.getCategory(), normalizedQuery))
                .filter(doc -> category == null || category.isBlank() || equalsIgnoreCase(doc.getCategory(), category))
                .filter(doc -> minPrice == null || (doc.getPrice() != null && doc.getPrice() >= minPrice))
                .filter(doc -> maxPrice == null || (doc.getPrice() != null && doc.getPrice() <= maxPrice))
                .sorted(Comparator.comparing(ProductDocument::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProductDocument::getPrice, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();

        return all.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Scans all indexed product names and returns those that start with
     * the given prefix (case-insensitive). Results are sorted alphabetically
     * and limited to the requested count (max 50).
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> autocomplete(String prefix, Integer size) {
        int limit = size == null || size <= 0 ? 10 : Math.min(size, 50);
        String normalizedPrefix = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);

        if (normalizedPrefix.isBlank()) {
            return List.of();
        }

        return StreamSupport.stream(productSearchRepository.findAll().spliterator(), false)
                .filter(doc -> !Boolean.TRUE.equals(doc.getIsDeleted()))
                .map(ProductDocument::getName)
                .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .distinct()
                .sorted()
                .limit(limit)
                .toList();
    }

    /**
     * Converts a JPA {@link Product} entity to an Elasticsearch
     * {@link ProductDocument} for indexing.
     *
     * @param product the product entity to convert
     * @return the corresponding Elasticsearch document
     */
    private ProductDocument toDocument(Product product) {
        return ProductDocument.builder()
                .uuid(product.getUuid())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .stock(product.getStock())
                .sellerUuid(product.getSellerUuid())
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .averageRating(product.getAverageRating())
                .isDeleted(product.getIsDeleted())
                .imageUrl(product.getImageUrl())
                .build();
    }

    /**
     * Converts an Elasticsearch {@link ProductDocument} to a
     * {@link ProductResponse} DTO for API consumers.
     *
     * @param doc the Elasticsearch document to convert
     * @return the corresponding response DTO
     */
    private ProductResponse toResponse(ProductDocument doc) {
        return ProductResponse.builder()
                .uuid(doc.getUuid())
                .name(doc.getName())
                .description(doc.getDescription())
                .category(doc.getCategory())
                .price(doc.getPrice())
                .stock(doc.getStock())
                .sellerUuid(doc.getSellerUuid())
                .status(doc.getStatus())
                .averageRating(doc.getAverageRating())
                .imageUrl(doc.getImageUrl())
                .build();
    }

    /**
     * Checks whether the source string contains the value (case-insensitive).
     *
     * @param source the string to search within
     * @param value  the substring to look for
     * @return {@code true} if source contains value, ignoring case
     */
    private boolean containsIgnoreCase(String source, String value) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(value);
    }

    /**
     * Null-safe, case-insensitive string equality check.
     *
     * @param left  first string
     * @param right second string
     * @return {@code true} if both are non-null and equal ignoring case
     */
    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
