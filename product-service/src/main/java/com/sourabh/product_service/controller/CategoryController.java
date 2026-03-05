package com.sourabh.product_service.controller;

import com.sourabh.product_service.dto.request.CreateCategoryRequest;
import com.sourabh.product_service.dto.response.CategoryResponse;
import com.sourabh.product_service.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing CRUD and hierarchy endpoints for product categories.
 * <p>
 * Write operations (create, update, delete, reorder) are restricted to users
 * with the {@code ADMIN} role, while read operations are publicly accessible.
 * </p>
 *
 * <p>Base path: {@code /api/category}</p>
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    /** Service encapsulating category business logic. */
    private final CategoryService categoryService;

    /**
     * Creates a new product category.
     *
     * @param request validated payload containing the category details
     * @return the newly created category with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    /**
     * Updates an existing category identified by its UUID.
     *
     * @param uuid    the unique identifier of the category to update
     * @param request validated payload with the new category details
     * @return the updated category
     */
    @PutMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String uuid,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(uuid, request));
    }

    /**
     * Deletes a category and all its descendants.
     *
     * @param uuid the unique identifier of the category to delete
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable String uuid) {
        categoryService.deleteCategory(uuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a single category by its UUID.
     *
     * @param uuid the unique identifier of the category
     * @return the matching category
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable String uuid) {
        return ResponseEntity.ok(categoryService.getCategory(uuid));
    }

    /**
     * Lists all root-level (top-level) categories.
     *
     * @return list of categories whose parent is {@code null}
     */
    @GetMapping("/root")
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    /**
     * Lists the immediate children of a given parent category.
     *
     * @param parentUuid UUID of the parent category
     * @return ordered list of child categories
     */
    @GetMapping("/{parentUuid}/children")
    public ResponseEntity<List<CategoryResponse>> getChildCategories(@PathVariable String parentUuid) {
        return ResponseEntity.ok(categoryService.getChildCategories(parentUuid));
    }

    /**
     * Returns the full category tree starting from all root categories.
     *
     * @return nested hierarchy of all active categories
     */
    @GetMapping("/hierarchy/all")
    public ResponseEntity<List<CategoryResponse>> getHierarchy() {
        return ResponseEntity.ok(categoryService.getHierarchy());
    }

    /**
     * Reorders child categories under a given parent.
     *
     * @param parentUuid       UUID of the parent category ({@code null} for root-level reorder)
     * @param childUuidsInOrder ordered list of child UUIDs defining the new display sequence
     * @return HTTP 204 No Content on success
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderCategories(
            @RequestParam(required = false) String parentUuid,
            @RequestBody List<String> childUuidsInOrder) {
        categoryService.reorderCategories(parentUuid, childUuidsInOrder);
        return ResponseEntity.noContent().build();
    }
}
