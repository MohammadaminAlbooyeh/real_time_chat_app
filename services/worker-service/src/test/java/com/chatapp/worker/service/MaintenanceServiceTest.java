package com.chatapp.worker.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        maintenanceService = new MaintenanceService(jdbcTemplate);
    }

    @Test
    void cleanupExpiredSessionsShouldDeleteOldRows() {
        maintenanceService.cleanupExpiredSessions();

        verify(jdbcTemplate).update(
            "DELETE FROM user_status_history WHERE changed_at < NOW() - INTERVAL '30 days'"
        );
    }

    @Test
    void archiveOldMessagesShouldUpdateOldMessages() {
        maintenanceService.archiveOldMessages();

        verify(jdbcTemplate).update(
            "UPDATE messages SET is_deleted = TRUE WHERE created_at < NOW() - INTERVAL '90 days' AND is_deleted = FALSE"
        );
    }
}
