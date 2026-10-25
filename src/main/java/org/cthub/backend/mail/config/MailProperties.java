package org.cthub.backend.mail.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Configuration for the mail infrastructure. See {@code application.yaml} under {@code app.mail} for
 * defaults and the per-profile overrides.
 */
@Data
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Master switch; when false, {@link org.cthub.backend.mail.MailService#enqueue} is a no-op (used in tests). */
    private boolean enabled = true;

    /** "zeptomail" | "smtp" | "logging". */
    private String provider = "logging";

    /** Public base URL used to build links (confirmation, reset, ...) embedded in mail bodies. */
    private String baseUrl = "http://localhost:5173";

    @NestedConfigurationProperty
    private From from = new From();

    private String replyTo;

    /** Dev safety net: when set, every outgoing mail is redirected to this address instead of the real recipient. */
    private String redirectAllTo;

    /**
     * Dev safety net: when non-empty, only recipients matching one of these entries are actually sent to;
     * everything else is cancelled in the outbox (never handed to the provider). Matched against the
     * <em>original</em> recipient, before {@link #redirectAllTo} rewrites it. Entries are matched
     * case-insensitively and may be a full address ({@code me@example.org}) or a domain
     * ({@code @example.org} / {@code example.org}). Leave empty in production.
     */
    private List<String> allowedRecipients = new ArrayList<>();

    @NestedConfigurationProperty
    private Outbox outbox = new Outbox();

    @NestedConfigurationProperty
    private ZeptoMail zeptomail = new ZeptoMail();

    /** True when the given recipient may be sent to under the current configuration. */
    public boolean isRecipientAllowed(String email) {
        if (allowedRecipients.isEmpty()) {
            return true;
        }
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String domain = normalized.contains("@") ? normalized.substring(normalized.indexOf('@')) : null;
        return allowedRecipients.stream()
            .map(entry -> entry.trim().toLowerCase(Locale.ROOT))
            .filter(entry -> !entry.isEmpty())
            .anyMatch(entry -> {
                String pattern = entry.contains("@") ? entry : "@" + entry;
                return normalized.equals(entry) || pattern.equals(domain);
            });
    }

    @PostConstruct
    void validate() {
        if ("zeptomail".equalsIgnoreCase(provider) && (zeptomail.getToken() == null || zeptomail.getToken().isBlank())) {
            throw new IllegalStateException(
                "app.mail.provider=zeptomail requires app.mail.zeptomail.token (ZEPTOMAIL_TOKEN) to be set");
        }
    }

    @Data
    public static class From {
        private String address = "noreply@challengeteamhub.org";
        private String name = "Challenge Team Hub";
    }

    @Data
    public static class Outbox {
        private Duration pollInterval = Duration.ofSeconds(15);
        private int batchSize = 25;
        private int maxAttempts = 5;
        private int retentionDays = 30;
    }

    @Data
    public static class ZeptoMail {
        private String baseUrl = "https://api.zeptomail.eu/v1.1/email";
        private String token = "";
    }
}
