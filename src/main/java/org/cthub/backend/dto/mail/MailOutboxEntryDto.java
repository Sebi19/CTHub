package org.cthub.backend.dto.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Admin-facing view of a {@link org.cthub.backend.mail.outbox.MailOutboxEntry}. Deliberately omits
 * the rendered HTML/text bodies to avoid exposing recipient-facing content (tokens, links) over the
 * admin API; attempts/lastError are enough to diagnose delivery problems.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MailOutboxEntryDto {
    private Long id;
    private String recipientEmail;
    private String subject;
    private String template;
    private String category;
    private String status;
    private int attempts;
    private String lastError;
    private Instant nextAttemptAt;
    private Instant createdAt;
    private Instant sentAt;
}
