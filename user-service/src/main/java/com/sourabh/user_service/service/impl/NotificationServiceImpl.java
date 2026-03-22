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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String userUuid, int page, int size) {
        Page<Notification> notifPage = notificationRepository
                .findByUserUuidOrderByCreatedAtDesc(userUuid, PageRequest.of(page, size));
        return toPageResponse(notifPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUnreadNotifications(String userUuid, int page, int size) {
        Page<Notification> notifPage = notificationRepository
                .findByUserUuidAndIsReadFalseOrderByCreatedAtDesc(userUuid, PageRequest.of(page, size));
        return toPageResponse(notifPage);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userUuid) {
        return notificationRepository.countByUserUuidAndIsReadFalse(userUuid);
    }

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

    @Override
    public void markAllAsRead(String userUuid) {
        int count = notificationRepository.markAllAsRead(userUuid);
        log.info("Marked {} notifications as read for user {}", count, userUuid);
    }

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
