package org.cthub.backend.mail.provider.logging;

import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.cthub.backend.mail.spi.MailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/**
 * Local/dev provider: never actually sends mail. Logs the envelope and dumps the rendered HTML to
 * {@code target/mail/} so developers can open it in a browser.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    private static final Path DUMP_DIR = Path.of("target", "mail");

    @Override
    public MailSendResult send(MailMessage message) throws MailDeliveryException {
        MailAddress to = message.to();
        log.info("[logging-mail] to={} subject=\"{}\" tags={}", to.email(), message.subject(), message.tags());

        String id = UUID.randomUUID().toString();
        try {
            Files.createDirectories(DUMP_DIR);
            Path file = DUMP_DIR.resolve(id + ".html");
            Files.writeString(file, message.htmlBody(), StandardCharsets.UTF_8);
            log.info("[logging-mail] dumped rendered HTML to {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("[logging-mail] could not dump rendered HTML", e);
        }

        return new MailSendResult(id, Instant.now());
    }

    @Override
    public String providerId() {
        return "logging";
    }
}
