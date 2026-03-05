package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Category} entities.
 * <p>
 * Provides CRUD operations plus custom query methods for navigating the
 * hierarchical category tree (root categories, children, full hierarchy).
 * Method names follow Spring Data naming conventions so that SQL is
 * auto-generated at runtime.
 * </p>
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds a category by its public UUID.
     *
     * @param uuid the universally unique identifier
     * @return an {@link Optional} containing the matching category, or empty
     */
    Optional<Category> findByUuid(String uuid);

    /**
     * Returns all active root categories (those with no parent).
     *
     * @return list of root-level categories that are currently active
     */
    List<Category> findByParentIsNullAndIsActiveTrue();

    /**
     * Returns the active children of a given parent category, ordered by display position.
     *
     * @param parentId database ID of the parent category
     * @return ordered list of active child categories
     */
    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrder(Long parentId);

    /**
     * Returns all active categories ordered by display position.
     *
     * @return flat list of every active category
     */
    List<Category> findAllByIsActiveTrueOrderByDisplayOrder();

    /**
     * Checks whether a category with the given name already exists under a specific parent.
     *
     * @param name     category name to look for
     * @param parentId database ID of the parent category
     * @return an {@link Optional} containing the duplicate, or empty
     */
    Optional<Category> findByNameAndParentId(String name, Long parentId);

    /**
     * Checks whether an active category exists with the given UUID.
     *
     * @param uuid the universally unique identifier
     * @return {@code true} if a matching active category exists
     */
    boolean existsByUuidAndIsActiveTrue(String uuid);

    /**
     * Loads the full category hierarchy starting from root categories.
     * Eagerly fetches one level of children to reduce N+1 queries.
     *
     * @return list of root categories with their immediate children pre-loaded
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findHierarchy();
}
