package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.NotificationResponse;
import com.sourabh.user_service.entity.NotificationType;

/**
 * Service interface for in-app user notifications.
 *
 * <p>Notifications are persisted in the database so users can view them
 * later.  Each notification carries a {@link NotificationType} (e.g.
 * {@code ORDER_CONFIRMED}, {@code ORDER_DELIVERED}) and an optional
 * {@code referenceId} linking back to the originating entity (order, ticket, etc.).</p>
 *
 * @see com.sourabh.user_service.service.impl.NotificationServiceImpl
 */
public interface NotificationService {

    /**
     * Creates and persists a new notification for the given user.
     *
     * @param userUuid    the UUID of the recipient user
     * @param type        the notification category
     * @param title       short summary displayed in notification lists
     * @param message     detailed notification body
     * @param referenceId optional external reference (e.g. order UUID)
     */
    void sendNotification(String userUuid, NotificationType type, String title, String message, String referenceId);

    /**
     * Returns a paginated list of all notifications for the user (read and unread).
     *
     * @param userUuid the UUID of the user
     * @param page     zero-based page index
     * @param size     page size
     * @return paginated {@link NotificationResponse} list ordered by creation date descending
     */
    PageResponse<NotificationResponse> getNotifications(String userUuid, int page, int size);

    /**
     * Returns a paginated list of unread notifications only.
     *
     * @param userUuid the UUID of the user
     * @param page     zero-based page index
     * @param size     page size
     * @return paginated {@link NotificationResponse} list of unread entries
     */
    PageResponse<NotificationResponse> getUnreadNotifications(String userUuid, int page, int size);

    /**
     * Returns the total count of unread notifications for badge display.
     *
     * @param userUuid the UUID of the user
     * @return the number of unread notifications
     */
    long getUnreadCount(String userUuid);

    /**
     * Marks a single notification as read.
     *
     * @param notificationUuid the UUID of the notification
     * @param userUuid         the UUID of the owning user (authorization check)
     * @throws RuntimeException if the notification does not exist or belongs to another user
     */
    void markAsRead(String notificationUuid, String userUuid);

    /**
     * Marks every unread notification for the user as read in a single bulk update.
     *
     * @param userUuid the UUID of the user
     */
    void markAllAsRead(String userUuid);
}
