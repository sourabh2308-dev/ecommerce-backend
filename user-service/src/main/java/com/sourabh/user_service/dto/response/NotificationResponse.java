package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response payload describing a notification entry.
 */
@Getter
@Builder
public class NotificationResponse {
    private String uuid;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
