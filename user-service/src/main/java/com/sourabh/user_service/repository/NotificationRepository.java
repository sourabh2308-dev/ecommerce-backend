package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    Page<Notification> findByUserUuidAndIsReadFalseOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    Optional<Notification> findByUuid(String uuid);

    long countByUserUuidAndIsReadFalse(String userUuid);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userUuid = :userUuid AND n.isRead = false")
    int markAllAsRead(@Param("userUuid") String userUuid);
}
