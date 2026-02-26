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

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;

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

    @Override
    public void removeProductFromIndex(String productUuid) {
        productSearchRepository.deleteById(productUuid);
        log.info("Removed product {} from Elasticsearch index", productUuid);
    }

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

    private boolean containsIgnoreCase(String source, String value) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(value);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
