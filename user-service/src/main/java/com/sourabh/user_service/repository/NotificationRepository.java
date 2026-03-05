package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Notification} entities.
 * <p>
 * Provides standard CRUD operations plus queries for listing,
 * filtering unread notifications, and bulk mark-as-read.
 * </p>
 *
 * @see Notification
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Returns all notifications for the given user, newest first.
     *
     * @param userUuid the user's UUID
     * @param pageable pagination parameters
     * @return page of notifications
     */
    Page<Notification> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    /**
     * Returns only unread notifications for the given user, newest first.
     *
     * @param userUuid the user's UUID
     * @param pageable pagination parameters
     * @return page of unread notifications
     */
    Page<Notification> findByUserUuidAndIsReadFalseOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    /**
     * Finds a notification by its public UUID.
     *
     * @param uuid the notification UUID
     * @return the matching notification, if any
     */
    Optional<Notification> findByUuid(String uuid);

    /**
     * Counts the number of unread notifications for the given user.
     *
     * @param userUuid the user's UUID
     * @return unread notification count
     */
    long countByUserUuidAndIsReadFalse(String userUuid);

    /**
     * Bulk-updates all unread notifications for the given user,
     * setting {@code isRead = true}.
     *
     * @param userUuid the user's UUID
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userUuid = :userUuid AND n.isRead = false")
    int markAllAsRead(@Param("userUuid") String userUuid);
}
