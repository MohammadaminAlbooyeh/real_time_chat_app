package com.chatapp.worker.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RoomCleanupServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private RoomCleanupService roomCleanupService;

    @BeforeEach
    void setUp() {
        roomCleanupService = new RoomCleanupService(jdbcTemplate);
    }

    @Test
    void cleanupAbandonedRoomsShouldDeleteRoomsWithoutMembers() {
        roomCleanupService.cleanupAbandonedRooms();

        verify(jdbcTemplate).update(
            "DELETE FROM rooms WHERE id NOT IN (SELECT DISTINCT room_id FROM room_members) AND created_at < NOW() - INTERVAL '30 days'"
        );
    }

    @Test
    void updateRoomLastActivityShouldUpdateTimestamp() {
        UUID roomId = UUID.randomUUID();

        roomCleanupService.updateRoomLastActivity(roomId);

        verify(jdbcTemplate).update(
            eq("UPDATE rooms SET updated_at = NOW() WHERE id = ?::uuid"),
            eq(roomId.toString())
        );
    }
}
