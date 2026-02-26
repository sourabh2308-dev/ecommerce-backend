package com.sourabh.product_service.search.repository;

import com.sourabh.product_service.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    List<ProductDocument> findByNameContainingOrDescriptionContaining(String name, String description);

    List<ProductDocument> findByCategoryAndIsDeletedFalse(String category);

    List<ProductDocument> findByNameContainingAndIsDeletedFalse(String keyword);
}
