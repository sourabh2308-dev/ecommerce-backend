package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.NotificationResponse;
import com.sourabh.user_service.entity.Notification;
import com.sourabh.user_service.entity.NotificationType;
import com.sourabh.user_service.repository.NotificationRepository;
import com.sourabh.user_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link NotificationService} for in-app user notifications.
 *
 * <p>Notifications are persisted to the {@code notifications} table, each carrying
 * a {@link NotificationType}, a title, a message body, and an optional
 * {@code referenceId} that links back to the originating entity (e.g. an order UUID).
 * The {@code isRead} flag tracks whether the user has acknowledged the notification.</p>
 *
 * <p>Bulk "mark all as read" is performed via a single repository update query
 * for efficiency rather than loading and saving each entity individually.</p>
 *
 * @see NotificationService
 * @see NotificationRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    /** Repository for {@link Notification} persistence operations. */
    private final NotificationRepository notificationRepository;

    /** {@inheritDoc} */
    @Override
    public void sendNotification(String userUuid, NotificationType type,
                                 String title, String message, String referenceId) {
        Notification notification = Notification.builder()
                .userUuid(userUuid)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);
        log.info("Notification sent: userUuid={}, type={}, title={}", userUuid, type, title);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String userUuid, int page, int size) {
        Page<Notification> notifPage = notificationRepository
                .findByUserUuidOrderByCreatedAtDesc(userUuid, PageRequest.of(page, size));
        return toPageResponse(notifPage);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUnreadNotifications(String userUuid, int page, int size) {
        Page<Notification> notifPage = notificationRepository
                .findByUserUuidAndIsReadFalseOrderByCreatedAtDesc(userUuid, PageRequest.of(page, size));
        return toPageResponse(notifPage);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userUuid) {
        return notificationRepository.countByUserUuidAndIsReadFalse(userUuid);
    }

    /** {@inheritDoc} */
    @Override
    public void markAsRead(String notificationUuid, String userUuid) {
        Notification notification = notificationRepository.findByUuid(notificationUuid)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUserUuid().equals(userUuid)) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void markAllAsRead(String userUuid) {
        int count = notificationRepository.markAllAsRead(userUuid);
        log.info("Marked {} notifications as read for user {}", count, userUuid);
    }

    /**
     * Converts a Spring Data {@link Page} of {@link Notification} entities
     * into a generic {@link PageResponse} of {@link NotificationResponse} DTOs.
     *
     * @param page the page of notification entities
     * @return the assembled page response
     */
    private PageResponse<NotificationResponse> toPageResponse(Page<Notification> page) {
        return PageResponse.<NotificationResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Maps a {@link Notification} entity to a {@link NotificationResponse} DTO.
     *
     * @param n the notification entity
     * @return the corresponding response DTO
     */
    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .uuid(n.getUuid())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
