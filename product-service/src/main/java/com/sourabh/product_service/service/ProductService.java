package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.CreateProductRequest;
import com.sourabh.product_service.dto.request.UpdateProductRequest;
import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.common.PageResponse;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request, String sellerUuid);

    ProductResponse updateProduct(String uuid,
                                  UpdateProductRequest request,
                                  String role,
                                  String sellerUuid);

    String approveProduct(String uuid);

    String blockProduct(String uuid);

    String softDeleteProduct(String uuid, String role, String sellerUuid);

    PageResponse<ProductResponse> listProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String role,
            String sellerUuid,
            String keyword);

    ProductResponse getProductByUuid(String uuid);

    String reduceStock(String productUuid, Integer quantity);

    void updateRating(String productUuid, Integer rating);

}
