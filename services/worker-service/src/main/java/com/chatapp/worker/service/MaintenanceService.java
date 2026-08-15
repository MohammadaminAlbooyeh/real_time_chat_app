package com.chatapp.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceService.class);

    private final JdbcTemplate jdbcTemplate;

    public MaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void cleanupExpiredSessions() {
        logger.info("Running scheduled maintenance: cleanup expired sessions");
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM user_status_history WHERE changed_at < NOW() - INTERVAL '30 days'"
            );
            logger.info("Expired sessions cleaned up, deleted {} rows", deleted);
        } catch (Exception e) {
            logger.error("Maintenance task failed: {}", e.getMessage());
        }
    }

    public void archiveOldMessages() {
        logger.info("Archiving messages older than 90 days");
        try {
            int updated = jdbcTemplate.update(
                "UPDATE messages SET is_deleted = TRUE WHERE created_at < NOW() - INTERVAL '90 days' AND is_deleted = FALSE"
            );
            logger.info("Old messages archived, updated {} rows", updated);
        } catch (Exception e) {
            logger.error("Archive task failed: {}", e.getMessage());
        }
    }
}
