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

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String uuid,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(uuid, request));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable String uuid) {
        categoryService.deleteCategory(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable String uuid) {
        return ResponseEntity.ok(categoryService.getCategory(uuid));
    }

    @GetMapping("/root")
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    @GetMapping("/{parentUuid}/children")
    public ResponseEntity<List<CategoryResponse>> getChildCategories(@PathVariable String parentUuid) {
        return ResponseEntity.ok(categoryService.getChildCategories(parentUuid));
    }

    @GetMapping("/hierarchy/all")
    public ResponseEntity<List<CategoryResponse>> getHierarchy() {
        return ResponseEntity.ok(categoryService.getHierarchy());
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderCategories(
            @RequestParam(required = false) String parentUuid,
            @RequestBody List<String> childUuidsInOrder) {
        categoryService.reorderCategories(parentUuid, childUuidsInOrder);
        return ResponseEntity.noContent().build();
    }
}
