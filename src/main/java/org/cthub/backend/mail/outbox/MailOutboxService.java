package org.cthub.backend.mail.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Owns the lifecycle of {@link MailOutboxEntry} rows: enqueue (inside the caller's transaction),
 * claiming due batches, and the terminal/retry transitions reported back by {@link MailDispatcher}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailOutboxService {

    /** Exponential-ish backoff schedule, indexed by attempt number (1-based). */
    private static final List<Duration> BACKOFF_SCHEDULE = List.of(
        Duration.ofMinutes(1),
        Duration.ofMinutes(5),
        Duration.ofMinutes(15),
        Duration.ofHours(1),
        Duration.ofHours(6)
    );

    private final MailOutboxRepository repository;
    private final MailProperties mailProperties;
    private final MeterRegistry meterRegistry;

    @jakarta.annotation.PostConstruct
    void registerPendingGauge() {
        meterRegistry.gauge("mail.outbox.pending", this, MailOutboxService::countPending);
    }

    private double countPending() {
        return repository.countByStatus(MailOutboxStatus.PENDING);
    }

    /**
     * Enqueues an already-rendered message. Must be called inside the caller's own transaction so
     * that, e.g., "user created" and "confirmation mail queued" commit atomically.
     */
    @Transactional
    public MailOutboxEntry enqueue(MailMessage rendered, MailTemplate template, Locale locale, String dedupeKey) {
        if (dedupeKey != null) {
            Optional<MailOutboxEntry> existing = repository.findByDedupeKey(dedupeKey);
            if (existing.isPresent()) {
                log.debug("Mail with dedupe key '{}' already queued (outbox id {}); skipping duplicate enqueue.",
                    dedupeKey, existing.get().getId());
                return existing.get();
            }
        }

        MailOutboxEntry entry = MailOutboxEntry.builder()
            .recipientEmail(rendered.to().email())
            .recipientName(rendered.to().name())
            .subject(rendered.subject())
            .htmlBody(rendered.htmlBody())
            .textBody(rendered.textBody())
            .template(template.name())
            .category(template.category())
            .locale(locale.toLanguageTag())
            .replyTo(rendered.replyTo() != null ? rendered.replyTo().email() : null)
            .cc(rendered.cc())
            .bcc(rendered.bcc())
            .headers(rendered.headers())
            .status(MailOutboxStatus.PENDING)
            .attempts(0)
            .nextAttemptAt(Instant.now())
            .dedupeKey(dedupeKey)
            .build();

        MailOutboxEntry saved = repository.save(entry);
        counter("mail.enqueued").increment();
        return saved;
    }

    /** Claims and marks {@code SENDING} the next due batch, inside one short transaction. */
    @Transactional
    public List<MailOutboxEntry> claimBatch() {
        List<MailOutboxEntry> due = repository.claimDueBatch(Instant.now(), mailProperties.getOutbox().getBatchSize());
        due.forEach(entry -> entry.setStatus(MailOutboxStatus.SENDING));
        return repository.saveAll(due);
    }

    @Transactional
    public void markSent(MailOutboxEntry entry, String providerId, MailSendResult result) {
        entry.setStatus(MailOutboxStatus.SENT);
        entry.setProvider(providerId);
        entry.setProviderMessageId(result.providerMessageId());
        entry.setSentAt(result.acceptedAt() != null ? result.acceptedAt() : Instant.now());
        entry.setLastError(null);
        repository.save(entry);
        counter("mail.sent").increment();
    }

    /**
     * Terminal, non-retryable suppression: the entry was never handed to the provider. Used by the
     * {@code app.mail.allowed-recipients} dev safety net.
     */
    @Transactional
    public void markCancelled(MailOutboxEntry entry, String reason) {
        entry.setStatus(MailOutboxStatus.CANCELLED);
        entry.setLastError(reason);
        repository.save(entry);
        counter("mail.cancelled").increment();
    }

    /**
     * Records a failed send attempt. Non-retryable errors fail immediately; retryable errors are
     * retried with backoff up to {@code app.mail.outbox.max-attempts}, after which they also become
     * terminal {@code FAILED}.
     */
    @Transactional
    public void markFailed(MailOutboxEntry entry, MailDeliveryException error) {
        entry.setAttempts(entry.getAttempts() + 1);
        entry.setLastError(error.getMessage());

        int maxAttempts = mailProperties.getOutbox().getMaxAttempts();
        if (!error.isRetryable() || entry.getAttempts() >= maxAttempts) {
            entry.setStatus(MailOutboxStatus.FAILED);
            counter("mail.failed").increment();
            log.warn("Mail outbox entry {} permanently failed after {} attempt(s): {}",
                entry.getId(), entry.getAttempts(), error.getMessage());
        } else {
            entry.setStatus(MailOutboxStatus.PENDING);
            entry.setNextAttemptAt(Instant.now().plus(backoffFor(entry.getAttempts())));
            counter("mail.retry").increment();
            log.info("Mail outbox entry {} failed (attempt {}/{}), retrying at {}: {}",
                entry.getId(), entry.getAttempts(), maxAttempts, entry.getNextAttemptAt(), error.getMessage());
        }
        repository.save(entry);
    }

    private Duration backoffFor(int attempt) {
        int index = Math.min(attempt - 1, BACKOFF_SCHEDULE.size() - 1);
        return BACKOFF_SCHEDULE.get(Math.max(index, 0));
    }

    @Transactional
    public MailOutboxEntry requeue(MailOutboxEntry entry) {
        entry.setStatus(MailOutboxStatus.PENDING);
        entry.setNextAttemptAt(Instant.now());
        entry.setLastError(null);
        return repository.save(entry);
    }

    private Counter counter(String name) {
        return meterRegistry.counter(name);
    }
}

