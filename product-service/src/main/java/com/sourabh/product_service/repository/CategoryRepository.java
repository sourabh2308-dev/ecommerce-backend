package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByUuid(String uuid);

    List<Category> findByParentIsNullAndIsActiveTrue();

    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrder(Long parentId);

    List<Category> findAllByIsActiveTrueOrderByDisplayOrder();

    Optional<Category> findByNameAndParentId(String name, Long parentId);

    boolean existsByUuidAndIsActiveTrue(String uuid);

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findHierarchy();
}
