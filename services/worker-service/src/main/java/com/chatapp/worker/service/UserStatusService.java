package com.chatapp.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserStatusService {

    private static final Logger logger = LoggerFactory.getLogger(UserStatusService.class);

    private final JdbcTemplate jdbcTemplate;

    public UserStatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void updateStaleStatuses() {
        logger.info("Updating stale user statuses");
        try {
            int updated = jdbcTemplate.update(
                "UPDATE users SET is_online = FALSE WHERE is_online = TRUE AND last_seen < NOW() - INTERVAL '5 minutes'"
            );
            logger.info("Stale statuses updated, {} users affected", updated);
        } catch (Exception e) {
            logger.error("Failed to update statuses: {}", e.getMessage());
        }
    }

    public void cleanupInactiveUsers() {
        logger.info("Cleaning up inactive users");
        try {
            int updated = jdbcTemplate.update(
                "UPDATE users SET is_active = FALSE WHERE is_active = TRUE AND last_seen < NOW() - INTERVAL '90 days'"
            );
            logger.info("Inactive users cleaned up, {} users affected", updated);
        } catch (Exception e) {
            logger.error("Failed to clean up inactive users: {}", e.getMessage());
        }
    }
}
