package org.cthub.backend.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Daily housekeeping: deletes notifications that were read more than
 * {@code app.notifications.retention-days} ago. Unread notifications are never deleted.
 */
@Component
@Slf4j
public class NotificationCleanupJob {

    private final NotificationRepository repository;
    private final int retentionDays;

    public NotificationCleanupJob(
        NotificationRepository repository,
        @Value("${app.notifications.retention-days:90}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 45 3 * * *") // daily at 03:45, after the mail outbox cleanup
    @Transactional
    public void cleanupReadNotifications() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = repository.deleteReadBefore(cutoff);
        if (deleted > 0) {
            log.info("Notification cleanup removed {} read notification(s) older than {} day(s).", deleted, retentionDays);
        }
    }
}
