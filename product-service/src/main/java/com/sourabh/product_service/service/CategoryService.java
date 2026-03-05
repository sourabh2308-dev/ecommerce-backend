package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.CreateCategoryRequest;
import com.sourabh.product_service.dto.response.CategoryResponse;

import java.util.List;

/**
 * Service interface for managing hierarchical product categories.
 *
 * <p>Provides CRUD operations and tree-management utilities for the
 * product category taxonomy. Categories support arbitrary nesting
 * via parent-child relationships, display-order sorting, and
 * soft-delete semantics (active/inactive flag).</p>
 *
 * @see com.sourabh.product_service.service.impl.CategoryServiceImpl
 * @see com.sourabh.product_service.entity.Category
 */
public interface CategoryService {

    /**
     * Creates a new product category.
     *
     * <p>If a parent UUID is supplied the category is inserted as a child;
     * otherwise it becomes a root-level category. Duplicate names within the
     * same parent are rejected.</p>
     *
     * @param request the creation payload (name, description, image URL,
     *                optional parent UUID, display order)
     * @return the newly created category
     * @throws IllegalArgumentException if the parent is not found or a
     *                                  duplicate name exists under that parent
     */
    CategoryResponse createCategory(CreateCategoryRequest request);

    /**
     * Updates an existing category identified by UUID.
     *
     * <p>Supports reparenting with circular-reference detection to
     * prevent invalid hierarchies.</p>
     *
     * @param uuid    the UUID of the category to update
     * @param request the update payload
     * @return the updated category
     * @throws IllegalArgumentException if the category or new parent is
     *                                  not found, or a circular reference is detected
     */
    CategoryResponse updateCategory(String uuid, CreateCategoryRequest request);

    /**
     * Soft-deletes a category by marking it inactive.
     *
     * <p>Deletion is blocked when the category still has active children;
     * children must be removed first.</p>
     *
     * @param uuid the UUID of the category to delete
     * @throws IllegalArgumentException if the category has sub-categories
     */
    void deleteCategory(String uuid);

    /**
     * Retrieves a single category by its UUID.
     *
     * @param uuid the UUID of the category
     * @return the matching {@link CategoryResponse}
     * @throws IllegalArgumentException if the category is not found
     */
    CategoryResponse getCategory(String uuid);

    /**
     * Returns all active root-level categories (those without a parent).
     *
     * @return list of root categories
     */
    List<CategoryResponse> getRootCategories();

    /**
     * Returns all active children of a given parent category,
     * ordered by their display order.
     *
     * @param parentUuid UUID of the parent category
     * @return ordered list of child categories
     * @throws IllegalArgumentException if the parent is not found
     */
    List<CategoryResponse> getChildCategories(String parentUuid);

    /**
     * Returns the complete category tree as a nested structure.
     *
     * <p>Root categories are returned with their children recursively
     * populated, filtered to active-only entries and sorted by display order.</p>
     *
     * @return nested list of root categories with children
     */
    List<CategoryResponse> getHierarchy();

    /**
     * Reorders child categories under a parent by setting display-order
     * values according to the position in the supplied list.
     *
     * @param parentUuid        UUID of the parent (null for root-level reorder)
     * @param childUuidsInOrder child UUIDs in the desired display order
     * @throws IllegalArgumentException if any child does not belong to the parent
     */
    void reorderCategories(String parentUuid, List<String> childUuidsInOrder);
}
