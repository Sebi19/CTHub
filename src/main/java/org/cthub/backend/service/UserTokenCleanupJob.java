package org.cthub.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.repository.UserTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Daily housekeeping: purges expired or already-consumed verification/reset tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserTokenCleanupJob {

    private final UserTokenRepository userTokenRepository;

    @Scheduled(cron = "0 0 4 * * *") // daily at 04:00
    @Transactional
    public void cleanupTokens() {
        int deleted = userTokenRepository.deleteExpiredOrConsumed(Instant.now());
        if (deleted > 0) {
            log.info("User token cleanup removed {} expired/consumed token(s).", deleted);
        }
    }
}
