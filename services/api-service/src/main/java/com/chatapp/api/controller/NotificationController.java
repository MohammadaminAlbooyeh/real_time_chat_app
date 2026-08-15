package com.chatapp.api.controller;

import com.chatapp.api.dto.NotificationListResponse;
import com.chatapp.api.dto.NotificationResponse;
import com.chatapp.api.model.User;
import com.chatapp.api.model.UserNotification;
import com.chatapp.api.repository.UserNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final UserNotificationRepository userNotificationRepository;

    public NotificationController(UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> listNotifications(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal User user) {

        PageRequest pageable = PageRequest.of(offset / limit, limit);
        Page<UserNotification> page;
        if (unreadOnly) {
            page = userNotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), pageable);
        } else {
            page = userNotificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }

        long total = userNotificationRepository.countByUserId(user.getId());
        long unreadCount = userNotificationRepository.countByUserIdAndIsReadFalse(user.getId());

        var items = page.getContent().stream()
            .map(NotificationResponse::from)
            .toList();

        return ResponseEntity.ok(new NotificationListResponse(items, total, unreadCount));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal User user) {
        long count = userNotificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Transactional
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID notificationId,
                                                            @AuthenticationPrincipal User user) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }

        notification.setRead(true);
        notification = userNotificationRepository.save(notification);
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    @Transactional
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@AuthenticationPrincipal User user) {
        userNotificationRepository.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
