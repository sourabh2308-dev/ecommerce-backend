package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.CreateCategoryRequest;
import com.sourabh.product_service.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(String uuid, CreateCategoryRequest request);

    void deleteCategory(String uuid);

    CategoryResponse getCategory(String uuid);

    List<CategoryResponse> getRootCategories();

    List<CategoryResponse> getChildCategories(String parentUuid);

    List<CategoryResponse> getHierarchy();

    void reorderCategories(String parentUuid, List<String> childUuidsInOrder);
}
