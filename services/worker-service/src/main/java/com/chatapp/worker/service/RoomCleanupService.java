package com.chatapp.worker.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(RoomCleanupService.class);

    private final JdbcTemplate jdbcTemplate;

    public RoomCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void cleanupAbandonedRooms() {
        logger.info("Cleaning up abandoned rooms");
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM rooms WHERE id NOT IN (SELECT DISTINCT room_id FROM room_members) AND created_at < NOW() - INTERVAL '30 days'"
            );
            logger.info("Abandoned rooms cleaned up, deleted {} rooms", deleted);
        } catch (Exception e) {
            logger.error("Failed to clean up rooms: {}", e.getMessage());
        }
    }

    public void updateRoomLastActivity(UUID roomId) {
        logger.info("Updating last activity for room {}", roomId);
        try {
            jdbcTemplate.update(
                "UPDATE rooms SET updated_at = NOW() WHERE id = ?::uuid",
                roomId.toString()
            );
        } catch (Exception e) {
            logger.error("Failed to update room activity: {}", e.getMessage());
        }
    }
}
