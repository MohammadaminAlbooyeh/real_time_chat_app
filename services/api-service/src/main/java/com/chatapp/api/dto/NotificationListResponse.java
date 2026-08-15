package com.chatapp.api.dto;

import java.util.List;

public record NotificationListResponse(
    List<NotificationResponse> items,
    long total,
    long unreadCount
) {}
