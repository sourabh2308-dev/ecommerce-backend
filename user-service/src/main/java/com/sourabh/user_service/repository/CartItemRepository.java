package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.CartItem;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserOrderByCreatedAtDesc(User user);

    Optional<CartItem> findByUserAndProductUuid(User user, String productUuid);

    Optional<CartItem> findByIdAndUser(Long id, User user);

    void deleteAllByUser(User user);

    int countByUser(User user);
}
