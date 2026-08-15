package com.chatapp.worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(jdbcTemplate);
    }

    @Test
    void sendPushNotificationShouldInsertRow() {
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
            .thenReturn("testuser");

        notificationService.sendPushNotification(userId, "Title", "Body", Map.of("key", "value"));

        verify(jdbcTemplate).update(
            eq("INSERT INTO user_notifications (user_id, title, body, data, notification_type) VALUES (?::uuid, ?, ?, ?::jsonb, 'push')"),
            eq(userId.toString()),
            eq("Title"),
            eq("Body"),
            anyString()
        );
    }

    @Test
    void sendEmailNotificationShouldInsertRow() {
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
            .thenReturn("user@example.com");

        notificationService.sendEmailNotification(userId, "Subject", "Body");

        verify(jdbcTemplate).update(
            eq("INSERT INTO user_notifications (user_id, title, body, data, notification_type) VALUES (?::uuid, ?, ?, ?::jsonb, 'email')"),
            eq(userId.toString()),
            eq("Subject"),
            eq("Body"),
            anyString()
        );
    }

    @Test
    void notifyOfflineUsersShouldCallSendForEachOfflineMember() {
        UUID roomId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(anyString(), eq(UUID.class), anyString()))
            .thenReturn(java.util.List.of(memberId));
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
            .thenReturn("Test Room")
            .thenReturn("testuser");

        notificationService.notifyOfflineUsers(roomId, "Hello", "Alice", java.util.List.of());

        verify(jdbcTemplate).queryForObject(
            eq("SELECT name FROM rooms WHERE id = ?::uuid"),
            eq(String.class),
            eq(roomId.toString())
        );
    }
}
