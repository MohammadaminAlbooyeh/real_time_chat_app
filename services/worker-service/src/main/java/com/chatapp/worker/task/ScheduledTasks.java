package com.chatapp.worker.task;

import com.chatapp.worker.service.MaintenanceService;
import com.chatapp.worker.service.RoomCleanupService;
import com.chatapp.worker.service.UserStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final MaintenanceService maintenanceService;
    private final UserStatusService userStatusService;
    private final RoomCleanupService roomCleanupService;

    public ScheduledTasks(MaintenanceService maintenanceService,
                          UserStatusService userStatusService,
                          RoomCleanupService roomCleanupService) {
        this.maintenanceService = maintenanceService;
        this.userStatusService = userStatusService;
        this.roomCleanupService = roomCleanupService;
    }

    @Scheduled(cron = "0 3 * * *")
    public void cleanupExpiredSessions() {
        logger.info("Scheduled task: cleanupExpiredSessions");
        maintenanceService.cleanupExpiredSessions();
    }

    @Scheduled(cron = "0 4 * * *")
    public void archiveOldMessages() {
        logger.info("Scheduled task: archiveOldMessages");
        maintenanceService.archiveOldMessages();
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void updateStaleStatuses() {
        logger.info("Scheduled task: updateStaleStatuses");
        userStatusService.updateStaleStatuses();
    }

    @Scheduled(cron = "0 5 * * *")
    public void cleanupInactiveUsers() {
        logger.info("Scheduled task: cleanupInactiveUsers");
        userStatusService.cleanupInactiveUsers();
    }

    @Scheduled(cron = "0 6 * * *")
    public void cleanupAbandonedRooms() {
        logger.info("Scheduled task: cleanupAbandonedRooms");
        roomCleanupService.cleanupAbandonedRooms();
    }
}
