package com.chatapp.api.dto;

import com.chatapp.api.model.UserNotification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String body,
    Object data,
    String notificationType,
    boolean isRead,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(UserNotification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getTitle(),
            notification.getBody(),
            notification.getData(),
            notification.getNotificationType(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
