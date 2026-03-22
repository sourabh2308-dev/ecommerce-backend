package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.NotificationResponse;
import com.sourabh.user_service.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(notificationService.getNotifications(userUuid, page, size));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping("/unread")
    public ResponseEntity<PageResponse<NotificationResponse>> getUnread(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userUuid, page, size));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        long count = notificationService.getUnreadCount(userUuid);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @PutMapping("/{uuid}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        notificationService.markAsRead(uuid, userUuid);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        notificationService.markAllAsRead(userUuid);
        return ResponseEntity.ok("All notifications marked as read");
    }
}
