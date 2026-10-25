package org.cthub.backend.mail.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.cthub.backend.mail.spi.MailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls {@link MailOutboxRepository} for due entries and hands them to the configured
 * {@link MailSender}. Runs on a small bounded executor so one slow provider call never blocks the
 * poll loop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailDispatcher {

    private final MailOutboxService outboxService;
    private final MailOutboxRepository outboxRepository;
    private final MailSender mailSender;
    private final MailProperties mailProperties;
    private final ThreadPoolTaskExecutor mailExecutor;

    @Scheduled(fixedDelayString = "${app.mail.outbox.poll-interval:15s}")
    public void dispatchDueMail() {
        if (!mailProperties.isEnabled()) {
            return;
        }

        List<MailOutboxEntry> batch = outboxService.claimBatch();
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Mail dispatcher claimed {} due entr{}", batch.size(), batch.size() == 1 ? "y" : "ies");
        for (MailOutboxEntry entry : batch) {
            Long id = entry.getId();
            mailExecutor.execute(() -> processEntry(id));
        }
    }

    private void processEntry(Long id) {
        MailOutboxEntry entry = outboxRepository.findById(id).orElse(null);
        if (entry == null) {
            log.warn("Mail outbox entry {} vanished before it could be sent.", id);
            return;
        }

        try {
            if (!mailProperties.isRecipientAllowed(entry.getRecipientEmail())) {
                // Dev safety net: checked against the *original* recipient, before redirect-all-to
                // rewrites it - otherwise every test mail would still be delivered to the dev inbox.
                log.warn("Mail outbox entry {} cancelled: recipient is not in app.mail.allowed-recipients.",
                    entry.getId());
                outboxService.markCancelled(entry, "Recipient not in app.mail.allowed-recipients");
                return;
            }
            MailMessage message = toMailMessage(entry);
            MailSendResult result = mailSender.send(message);
            // Never log full mail bodies or tokens - only the outbox id and provider message id.
            log.info("Sent mail outbox entry {} via {} (provider message id {}).",
                entry.getId(), mailSender.providerId(), result.providerMessageId());
            outboxService.markSent(entry, mailSender.providerId(), result);
        } catch (MailDeliveryException e) {
            outboxService.markFailed(entry, e);
        } catch (Exception e) {
            // Defensive: an unexpected (non-provider) failure should not be silently retried forever
            // without ever being recorded.
            log.error("Unexpected error sending mail outbox entry {}", entry.getId(), e);
            outboxService.markFailed(entry, new MailDeliveryException(e.getMessage(), true, null, e));
        }
    }

    private MailMessage toMailMessage(MailOutboxEntry entry) {
        MailProperties.From from = mailProperties.getFrom();
        MailAddress fromAddress = new MailAddress(from.getAddress(), from.getName());

        String redirectAllTo = mailProperties.getRedirectAllTo();
        boolean redirecting = redirectAllTo != null && !redirectAllTo.isBlank();

        MailAddress to = redirecting
            ? MailAddress.of(redirectAllTo)
            : new MailAddress(entry.getRecipientEmail(), entry.getRecipientName());

        String replyToEmail = entry.getReplyTo() != null ? entry.getReplyTo() : mailProperties.getReplyTo();
        MailAddress replyTo = replyToEmail != null && !replyToEmail.isBlank() ? MailAddress.of(replyToEmail) : null;

        // When redirecting to a dev/test inbox, never also leak the real cc/bcc recipients.
        return new MailMessage(
            fromAddress,
            to,
            redirecting ? List.of() : entry.getCc(),
            redirecting ? List.of() : entry.getBcc(),
            replyTo,
            entry.getSubject(),
            entry.getHtmlBody(),
            entry.getTextBody(),
            List.of(),
            entry.getHeaders(),
            List.of(entry.getTemplate())
        );
    }
}
