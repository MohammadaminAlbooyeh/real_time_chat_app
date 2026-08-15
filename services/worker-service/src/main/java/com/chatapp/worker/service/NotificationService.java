package com.chatapp.worker.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final JdbcTemplate jdbcTemplate;

    public NotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void sendPushNotification(UUID userId, String title, String body, Map<String, Object> data) {
        logger.info("Sending push notification to user {}: {}", userId, title);
        try {
            String username = jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE id = ?::uuid",
                String.class,
                userId.toString()
            );
            if (username == null) {
                logger.warn("User {} not found for push notification", userId);
                return;
            }
            String dataStr = data != null ? data.toString() : "{}";
            jdbcTemplate.update(
                "INSERT INTO user_notifications (user_id, title, body, data, notification_type) VALUES (?::uuid, ?, ?, ?::jsonb, 'push')",
                userId.toString(), title, body, dataStr
            );
            logger.info("Push notification stored for user {}", userId);
        } catch (Exception e) {
            logger.error("Failed to send push notification to user {}: {}", userId, e.getMessage());
        }
    }

    public void sendEmailNotification(UUID userId, String subject, String body) {
        logger.info("Sending email to user {}: {}", userId, subject);
        try {
            String email = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?::uuid",
                String.class,
                userId.toString()
            );
            if (email == null) {
                logger.warn("User {} not found for email notification", userId);
                return;
            }
            String dataStr = "{\"to\":\"" + email + "\"}";
            jdbcTemplate.update(
                "INSERT INTO user_notifications (user_id, title, body, data, notification_type) VALUES (?::uuid, ?, ?, ?::jsonb, 'email')",
                userId.toString(), subject, body, dataStr
            );
            logger.info("Email notification stored for user {}", email);
        } catch (Exception e) {
            logger.error("Failed to send email notification: {}", e.getMessage());
        }
    }

    public void notifyOfflineUsers(UUID roomId, String messagePreview, String senderName, List<UUID> excludeUserIds) {
        logger.info("Notifying offline users in room {} about message from {}", roomId, senderName);
        try {
            List<UUID> memberIds = jdbcTemplate.queryForList(
                """
                SELECT u.id FROM room_members rm
                JOIN users u ON u.id = rm.user_id
                WHERE rm.room_id = ?::uuid
                AND u.is_online = FALSE
                AND u.is_active = TRUE
                """,
                UUID.class,
                roomId.toString()
            );

            String roomName = jdbcTemplate.queryForObject(
                "SELECT name FROM rooms WHERE id = ?::uuid",
                String.class,
                roomId.toString()
            );
            if (roomName == null) {
                roomName = "Unknown Room";
            }

            Set<UUID> excludeSet = excludeUserIds != null
                ? excludeUserIds.stream().collect(Collectors.toSet())
                : Set.of();

            for (UUID memberId : memberIds) {
                if (excludeSet.contains(memberId)) {
                    continue;
                }
                sendPushNotification(
                    memberId,
                    "New message in " + roomName,
                    senderName + ": " + (messagePreview.length() > 100 ? messagePreview.substring(0, 100) : messagePreview),
                    Map.of("room_id", roomId.toString(), "type", "new_message")
                );
            }
            logger.info("Queued offline notifications for {} users in room {}", memberIds.size(), roomId);
        } catch (Exception e) {
            logger.error("Failed to notify offline users in room {}: {}", roomId, e.getMessage());
        }
    }
}
