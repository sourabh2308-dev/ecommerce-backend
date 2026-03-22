package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.OrderEventOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderEventOutboxRepository extends JpaRepository<OrderEventOutbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEventOutbox o WHERE o.id = :id")
    Optional<OrderEventOutbox> findByIdForUpdate(@Param("id") Long id);

    List<OrderEventOutbox> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}