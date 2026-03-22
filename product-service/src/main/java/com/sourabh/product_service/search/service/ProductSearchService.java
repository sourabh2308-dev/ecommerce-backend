package com.sourabh.product_service.search.service;

import com.sourabh.product_service.dto.response.ProductResponse;

import java.util.List;

public interface ProductSearchService {

    void indexAllProducts();

    void indexProductByUuid(String productUuid);

    void removeProductFromIndex(String productUuid);

    List<ProductResponse> search(String query, String category, Double minPrice, Double maxPrice, Integer size);

    List<String> autocomplete(String prefix, Integer size);
}
