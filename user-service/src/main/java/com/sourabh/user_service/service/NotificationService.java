package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.NotificationResponse;
import com.sourabh.user_service.entity.NotificationType;

public interface NotificationService {

    void sendNotification(String userUuid, NotificationType type, String title, String message, String referenceId);

    PageResponse<NotificationResponse> getNotifications(String userUuid, int page, int size);

    PageResponse<NotificationResponse> getUnreadNotifications(String userUuid, int page, int size);

    long getUnreadCount(String userUuid);

    void markAsRead(String notificationUuid, String userUuid);

    void markAllAsRead(String userUuid);
}
