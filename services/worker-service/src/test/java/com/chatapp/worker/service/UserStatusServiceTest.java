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
class UserStatusServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private UserStatusService userStatusService;

    @BeforeEach
    void setUp() {
        userStatusService = new UserStatusService(jdbcTemplate);
    }

    @Test
    void updateStaleStatusesShouldSetOfflineForStaleUsers() {
        userStatusService.updateStaleStatuses();

        verify(jdbcTemplate).update(
            "UPDATE users SET is_online = FALSE WHERE is_online = TRUE AND last_seen < NOW() - INTERVAL '5 minutes'"
        );
    }

    @Test
    void cleanupInactiveUsersShouldSetInactiveForOldUsers() {
        userStatusService.cleanupInactiveUsers();

        verify(jdbcTemplate).update(
            "UPDATE users SET is_active = FALSE WHERE is_active = TRUE AND last_seen < NOW() - INTERVAL '90 days'"
        );
    }
}
