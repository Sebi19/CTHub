package org.cthub.backend.mail.outbox;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailOutboxServiceTest {

    @Mock
    private MailOutboxRepository repository;

    private MailOutboxService service;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.getOutbox().setMaxAttempts(3);
        service = new MailOutboxService(repository, properties, new SimpleMeterRegistry());
    }

    private MailMessage rendered() {
        return new MailMessage(null, MailAddress.of("test@example.org"), null, null, null,
            "Subject", "<p>html</p>", "text", null, null, null);
    }

    @Test
    void enqueueSkipsDuplicateWhenDedupeKeyAlreadyQueued() {
        MailOutboxEntry existing = MailOutboxEntry.builder().id(42L).build();
        when(repository.findByDedupeKey("dup-key")).thenReturn(Optional.of(existing));

        MailOutboxEntry result = service.enqueue(rendered(), MailTemplate.WELCOME, Locale.GERMAN, "dup-key");

        assertThat(result.getId()).isEqualTo(42L);
        verify(repository, never()).save(any());
    }

    @Test
    void enqueueSavesNewPendingEntryWhenNoDedupeKeyMatch() {
        when(repository.findByDedupeKey(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MailOutboxEntry result = service.enqueue(rendered(), MailTemplate.WELCOME, Locale.GERMAN, "some-key");

        assertThat(result.getStatus()).isEqualTo(MailOutboxStatus.PENDING);
        assertThat(result.getAttempts()).isZero();
    }

    @Test
    void markFailedRetriesWithBackoffWhenRetryableAndBelowMaxAttempts() {
        MailOutboxEntry entry = MailOutboxEntry.builder()
            .id(1L).status(MailOutboxStatus.SENDING).attempts(0).build();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markFailed(entry, new MailDeliveryException("timeout", true, null));

        assertThat(entry.getStatus()).isEqualTo(MailOutboxStatus.PENDING);
        assertThat(entry.getAttempts()).isEqualTo(1);
        assertThat(entry.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void markFailedBecomesTerminalAfterExhaustingMaxAttempts() {
        MailOutboxEntry entry = MailOutboxEntry.builder()
            .id(1L).status(MailOutboxStatus.SENDING).attempts(2).build(); // next attempt will be 3rd = maxAttempts
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markFailed(entry, new MailDeliveryException("timeout", true, null));

        assertThat(entry.getStatus()).isEqualTo(MailOutboxStatus.FAILED);
        assertThat(entry.getAttempts()).isEqualTo(3);
    }

    @Test
    void markFailedBecomesTerminalImmediatelyWhenNotRetryable() {
        MailOutboxEntry entry = MailOutboxEntry.builder()
            .id(1L).status(MailOutboxStatus.SENDING).attempts(0).build();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markFailed(entry, new MailDeliveryException("rejected", false, "INVALID_RECIPIENT"));

        assertThat(entry.getStatus()).isEqualTo(MailOutboxStatus.FAILED);
        assertThat(entry.getAttempts()).isEqualTo(1);
    }

    @Test
    void markSentRecordsProviderDetails() {
        MailOutboxEntry entry = MailOutboxEntry.builder().id(1L).status(MailOutboxStatus.SENDING).build();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markSent(entry, "logging", new MailSendResult("msg-123", Instant.now()));

        assertThat(entry.getStatus()).isEqualTo(MailOutboxStatus.SENT);
        assertThat(entry.getProviderMessageId()).isEqualTo("msg-123");
        assertThat(entry.getProvider()).isEqualTo("logging");
    }

    @Test
    void requeueResetsToPendingAndClearsLastError() {
        MailOutboxEntry entry = MailOutboxEntry.builder()
            .id(1L).status(MailOutboxStatus.FAILED).lastError("boom").attempts(5).build();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MailOutboxEntry result = service.requeue(entry);

        assertThat(result.getStatus()).isEqualTo(MailOutboxStatus.PENDING);
        assertThat(result.getLastError()).isNull();
    }
}
