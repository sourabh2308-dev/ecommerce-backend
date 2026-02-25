package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.SellerDetail;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Data Repository - Provides database access via Spring Data JPA
public interface SellerDetailRepository extends JpaRepository<SellerDetail, Long> {

    Optional<SellerDetail> findByUser(User user);

    boolean existsByUser(User user);
}
