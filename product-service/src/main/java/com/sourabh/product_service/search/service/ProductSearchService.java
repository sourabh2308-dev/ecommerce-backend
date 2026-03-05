package com.sourabh.product_service.search.service;

import com.sourabh.product_service.dto.response.ProductResponse;

import java.util.List;

/**
 * Service interface for Elasticsearch-backed product search and indexing.
 * <p>
 * Defines operations for (re)indexing products from the PostgreSQL source
 * of truth into Elasticsearch, as well as multi-criteria search and
 * autocomplete queries against the search index.
 * </p>
 */
public interface ProductSearchService {

    /**
     * Indexes (or re-indexes) all non-deleted products from the database
     * into the Elasticsearch {@code products} index.
     */
    void indexAllProducts();

    /**
     * Indexes a single product identified by its UUID. If the product has
     * been deleted, it is removed from the index instead.
     *
     * @param productUuid UUID of the product to index
     */
    void indexProductByUuid(String productUuid);

    /**
     * Removes a product document from the Elasticsearch index.
     *
     * @param productUuid UUID of the product to remove
     */
    void removeProductFromIndex(String productUuid);

    /**
     * Searches the Elasticsearch index with optional free-text query,
     * category filter, and price-range constraints.
     *
     * @param query    free-text search string (matched against name, description, category)
     * @param category optional exact category name filter
     * @param minPrice optional minimum price (inclusive)
     * @param maxPrice optional maximum price (inclusive)
     * @param size     maximum number of results to return
     * @return list of matching products mapped to response DTOs
     */
    List<ProductResponse> search(String query, String category, Double minPrice, Double maxPrice, Integer size);

    /**
     * Returns product name suggestions matching the given prefix.
     * Used for search-as-you-type autocomplete.
     *
     * @param prefix the prefix to match against product names
     * @param size   maximum number of suggestions to return
     * @return list of matching product names
     */
    List<String> autocomplete(String prefix, Integer size);
}
