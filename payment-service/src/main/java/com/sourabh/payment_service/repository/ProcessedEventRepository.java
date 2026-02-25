package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

// Data Repository - Provides database access via Spring Data JPA
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventId(String eventId);
}
