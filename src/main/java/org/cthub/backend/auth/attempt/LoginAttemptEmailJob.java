package org.cthub.backend.auth.attempt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Sends the e-mailed link for attempts whose push head start ran out without anybody opening the
 * push. Push subscriptions can outlive their device (a dead phone never answers 410), so the mail
 * is the guaranteed channel.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptEmailJob {

    private final LoginAttemptRepository repository;
    private final LoginAttemptService service;
    private final Clock clock;

    @Scheduled(fixedDelayString = "PT2S", initialDelayString = "PT10S")
    public void sendDueLinks() {
        for (UUID id : repository.findEmailDue(clock.instant())) {
            try {
                service.sendLinkIfDue(id);
            } catch (RuntimeException e) {
                log.warn("Could not send the sign-in link of attempt {}.", id, e);
            }
        }
    }
}
