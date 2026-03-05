package com.sourabh.product_service.search.repository;

import com.sourabh.product_service.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data Elasticsearch repository for {@link ProductDocument} entities.
 * <p>
 * Provides CRUD operations against the {@code products} Elasticsearch index
 * and derived query methods for keyword search and category filtering.
 * </p>
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    /**
     * Full-text search across product name and description fields.
     *
     * @param name        keyword to match in the product name
     * @param description keyword to match in the product description
     * @return list of matching product documents
     */
    List<ProductDocument> findByNameContainingOrDescriptionContaining(String name, String description);

    /**
     * Returns non-deleted products in the specified category.
     *
     * @param category exact category name to filter by
     * @return list of matching product documents
     */
    List<ProductDocument> findByCategoryAndIsDeletedFalse(String category);

    /**
     * Returns non-deleted products whose name contains the given keyword.
     *
     * @param keyword substring to search for in product names
     * @return list of matching product documents
     */
    List<ProductDocument> findByNameContainingAndIsDeletedFalse(String keyword);
}
