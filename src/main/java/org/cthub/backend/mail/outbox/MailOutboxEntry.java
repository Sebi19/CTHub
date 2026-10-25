package org.cthub.backend.mail.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailCategory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A single outbound mail, durably queued inside the caller's transaction (see
 * {@link MailOutboxService#enqueue}) and later picked up by {@link MailDispatcher}.
 */
@Entity
@Table(name = "mail_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailOutboxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mail_outbox_seq_gen")
    @SequenceGenerator(name = "mail_outbox_seq_gen", sequenceName = "mail_outbox_seq")
    private Long id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(nullable = false, length = 998)
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "text")
    private String htmlBody;

    @Column(name = "text_body", nullable = false, columnDefinition = "text")
    private String textBody;

    /** {@link org.cthub.backend.mail.model.MailTemplate} name, kept for observability/debugging. */
    @Column(nullable = false)
    private String template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailCategory category;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "reply_to")
    private String replyTo;

    @Convert(converter = MailAddressListConverter.class)
    @Column(columnDefinition = "text")
    private List<MailAddress> cc;

    @Convert(converter = MailAddressListConverter.class)
    @Column(columnDefinition = "text")
    private List<MailAddress> bcc;

    @Convert(converter = MailHeadersConverter.class)
    @Column(columnDefinition = "text")
    private Map<String, String> headers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MailOutboxStatus status;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    private String provider;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    /** Unique key used to prevent duplicate sends (e.g. a double-clicked "resend confirmation"). */
    @Column(name = "dedupe_key", unique = true)
    private String dedupeKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
