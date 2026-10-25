package org.cthub.backend.mail.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.config.MailProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Daily housekeeping: deletes {@code SENT} outbox rows older than {@code app.mail.outbox.retention-days}.
 * {@code FAILED} rows are kept indefinitely for support/debugging (see admin outbox endpoints).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailOutboxCleanupJob {

    private final MailOutboxRepository repository;
    private final MailProperties mailProperties;

    @Scheduled(cron = "0 30 3 * * *") // daily at 03:30
    @Transactional
    public void cleanupSentMail() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(mailProperties.getOutbox().getRetentionDays()));
        long deleted = repository.deleteByStatusAndSentAtBefore(MailOutboxStatus.SENT, cutoff);
        if (deleted > 0) {
            log.info("Mail outbox cleanup removed {} SENT entr{} older than {} day(s).",
                deleted, deleted == 1 ? "y" : "ies", mailProperties.getOutbox().getRetentionDays());
        }
    }
}
