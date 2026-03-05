package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.dto.request.CreateCategoryRequest;
import com.sourabh.product_service.dto.response.CategoryResponse;
import com.sourabh.product_service.entity.Category;
import com.sourabh.product_service.repository.CategoryRepository;
import com.sourabh.product_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CategoryService} for hierarchical category management.
 *
 * <p>All public methods run inside a transaction (class-level {@code @Transactional}).
 * Read-only operations override with {@code readOnly = true} to allow
 * database-level optimisations such as replica routing.</p>
 *
 * <p>Categories form an adjacency-list tree stored in PostgreSQL. The service
 * enforces uniqueness of names within the same parent, prevents circular
 * references on reparenting, and supports recursive hierarchy retrieval.</p>
 *
 * @see CategoryService
 * @see CategoryRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    /** JPA repository for {@link Category} persistence. */
    private final CategoryRepository categoryRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Validates that the optional parent exists and that no sibling
     * category shares the same name. Defaults display order to 0 when
     * not provided.</p>
     */
    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category parent = null;
        if (request.getParentUuid() != null && !request.getParentUuid().isBlank()) {
            parent = categoryRepository.findByUuid(request.getParentUuid())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }

        if (categoryRepository.findByNameAndParentId(request.getName(), 
                parent != null ? parent.getId() : null).isPresent()) {
            throw new IllegalArgumentException("Category name already exists in this parent");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .parent(parent)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        categoryRepository.save(category);
        return mapToResponse(category);
    }

    /**
     * {@inheritDoc}
     *
     * <p>When a new parent UUID is supplied the method performs circular-reference
     * detection by walking the subtree of the current category and rejecting the
     * update if the proposed parent is found among its descendants.</p>
     */
    @Override
    public CategoryResponse updateCategory(String uuid, CreateCategoryRequest request) {
        Category category = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (request.getParentUuid() != null) {
            if (request.getParentUuid().isBlank()) {
                category.setParent(null);
            } else {
                if (request.getParentUuid().equals(uuid)) {
                    throw new IllegalArgumentException("Cannot set category as its own parent");
                }
                Category newParent = categoryRepository.findByUuid(request.getParentUuid())
                        .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
                if (isDescendant(newParent, category)) {
                    throw new IllegalArgumentException("Cannot set a descendant as parent (circular reference)");
                }
                category.setParent(newParent);
            }
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        categoryRepository.save(category);
        return mapToResponse(category);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Soft-deletes by setting {@code isActive = false}. Rejects the
     * operation when the category still has child categories.</p>
     */
    @Override
    public void deleteCategory(String uuid) {
        Category category = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.getChildren().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete category with sub-categories. Delete children first.");
        }

        category.setActive(false);
        categoryRepository.save(category);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(String uuid) {
        return categoryRepository.findByUuid(uuid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNullAndIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(String parentUuid) {
        Category parent = categoryRepository.findByUuid(parentUuid)
                .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));

        return categoryRepository.findByParentIdAndIsActiveTrueOrderByDisplayOrder(parent.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses the repository's {@code findHierarchy()} query to fetch root
     * categories, then recursively maps children via
     * {@link #mapToHierarchyResponse(Category)}.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getHierarchy() {
        return categoryRepository.findHierarchy()
                .stream()
                .map(this::mapToHierarchyResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates the ordered UUID list, validates parent ownership for
     * each child, and persists the new display-order index.</p>
     */
    @Override
    public void reorderCategories(String parentUuid, List<String> childUuidsInOrder) {
        Category parent = null;
        if (parentUuid != null && !parentUuid.isBlank()) {
            parent = categoryRepository.findByUuid(parentUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }

        for (int i = 0; i < childUuidsInOrder.size(); i++) {
            String childUuid = childUuidsInOrder.get(i);
            Category child = categoryRepository.findByUuid(childUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Child category not found: " + childUuid));

            if ((parent == null && child.getParent() != null) ||
                    (parent != null && (child.getParent() == null || !child.getParent().getId().equals(parent.getId())))) {
                throw new IllegalArgumentException("Child does not belong to parent");
            }

            child.setDisplayOrder(i);
            categoryRepository.save(child);
        }
    }

    /**
     * Recursively checks whether {@code potentialParent} is a descendant
     * of {@code category} in the category tree.
     *
     * @param potentialParent the category to look for in the subtree
     * @param category        the root of the subtree to search
     * @return {@code true} if {@code potentialParent} is found among descendants
     */
    private boolean isDescendant(Category potentialParent, Category category) {
        for (Category child : category.getChildren()) {
            if (child.getId().equals(potentialParent.getId())) {
                return true;
            }
            if (isDescendant(potentialParent, child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Maps a {@link Category} entity to a flat {@link CategoryResponse} DTO.
     *
     * @param category the entity to convert
     * @return the corresponding response DTO
     */
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .uuid(category.getUuid())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentUuid(category.getParent() != null ? category.getParent().getUuid() : null)
                .displayOrder(category.getDisplayOrder())
                .isActive(category.isActive())
                .createdAt(category.getCreatedAt())
                .build();
    }

    /**
     * Recursively maps a {@link Category} and its active children into a
     * nested {@link CategoryResponse} tree, sorted by display order.
     *
     * @param category the root category of the subtree
     * @return a response DTO with recursively populated children
     */
    private CategoryResponse mapToHierarchyResponse(Category category) {
        return CategoryResponse.builder()
                .uuid(category.getUuid())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentUuid(category.getParent() != null ? category.getParent().getUuid() : null)
                .displayOrder(category.getDisplayOrder())
                .isActive(category.isActive())
                .children(category.getChildren().stream()
                        .filter(Category::isActive)
                        .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                        .map(this::mapToHierarchyResponse)
                        .collect(Collectors.toList()))
                .createdAt(category.getCreatedAt())
                .build();
    }
}
