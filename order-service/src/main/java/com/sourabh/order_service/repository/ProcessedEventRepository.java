package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

// Data Repository - Provides database access via Spring Data JPA
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventId(String eventId);
}
