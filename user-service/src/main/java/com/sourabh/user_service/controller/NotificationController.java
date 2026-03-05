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

/**
 * REST controller for in-app notification operations.
 * <p>
 * Provides endpoints for any authenticated user (BUYER, SELLER, or
 * ADMIN) to retrieve, read, and manage their notifications.
 * </p>
 *
 * <p>Base path: {@code /api/user/notifications}</p>
 *
 * @see NotificationService
 */
@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    /** Service layer handling notification business logic. */
    private final NotificationService notificationService;

    /**
     * Returns a paginated list of all notifications for the
     * authenticated user, newest first.
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 20)
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return paginated {@link NotificationResponse} list
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(notificationService.getNotifications(userUuid, page, size));
    }

    /**
     * Returns a paginated list of unread notifications for the
     * authenticated user, newest first.
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 20)
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return paginated {@link NotificationResponse} list (unread only)
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping("/unread")
    public ResponseEntity<PageResponse<NotificationResponse>> getUnread(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userUuid, page, size));
    }

    /**
     * Returns the count of unread notifications for the authenticated user.
     *
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return map with key {@code unreadCount}
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        long count = notificationService.getUnreadCount(userUuid);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Marks a single notification as read.
     *
     * @param uuid        UUID of the notification to mark
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return confirmation message
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @PutMapping("/{uuid}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        notificationService.markAsRead(uuid, userUuid);
        return ResponseEntity.ok("Notification marked as read");
    }

    /**
     * Marks all notifications belonging to the authenticated user as read.
     *
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return confirmation message
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        notificationService.markAllAsRead(userUuid);
        return ResponseEntity.ok("All notifications marked as read");
    }
}
