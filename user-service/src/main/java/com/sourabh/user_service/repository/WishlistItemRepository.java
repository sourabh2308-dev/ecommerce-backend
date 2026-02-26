package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserOrderByCreatedAtDesc(User user);

    Optional<WishlistItem> findByUserAndProductUuid(User user, String productUuid);

    boolean existsByUserAndProductUuid(User user, String productUuid);

    void deleteByUserAndProductUuid(User user, String productUuid);
}
