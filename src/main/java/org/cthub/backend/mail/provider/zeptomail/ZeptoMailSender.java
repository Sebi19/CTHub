package org.cthub.backend.mail.provider.zeptomail;

import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailAttachment;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailSendResult;
import org.cthub.backend.mail.spi.MailDeliveryException;
import org.cthub.backend.mail.spi.MailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Adapter for the ZeptoMail transactional e-mail API. No vendor SDK: plain REST via
 * {@link RestClient}, so switching provider later never needs a new dependency - just a new adapter.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "zeptomail")
public class ZeptoMailSender implements MailSender {

    private final RestClient restClient;

    public ZeptoMailSender(MailProperties mailProperties) {
        this.restClient = RestClient.builder()
            .baseUrl(mailProperties.getZeptomail().getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-enczapikey " + mailProperties.getZeptomail().getToken())
            .build();
    }

    @Override
    public MailSendResult send(MailMessage message) throws MailDeliveryException {
        ZeptoMailRequest request = toRequest(message);
        try {
            ResponseEntity<ZeptoMailResponse> response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ZeptoMailResponse.class);

            ZeptoMailResponse body = response.getBody();
            String messageId = (body != null && body.data() != null && !body.data().isEmpty())
                ? body.data().get(0).messageId()
                : null;
            return new MailSendResult(messageId, Instant.now());
        } catch (HttpStatusCodeException e) {
            throw new MailDeliveryException(
                "ZeptoMail send failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(),
                isRetryable(e.getStatusCode()),
                String.valueOf(e.getStatusCode().value()),
                e);
        } catch (RestClientException e) {
            // Network-level failure (timeout, DNS, connection refused, ...): assume transient.
            throw new MailDeliveryException("ZeptoMail send failed: " + e.getMessage(), true, null, e);
        }
    }

    private boolean isRetryable(HttpStatusCode status) {
        // Rate limiting and server-side failures are worth retrying; 4xx client errors
        // (invalid recipient, bad request, auth failure, ...) are not.
        return status.is5xxServerError() || status.value() == 429;
    }

    private ZeptoMailRequest toRequest(MailMessage message) {
        List<ZeptoMailRequest.Attachment> attachments = message.attachments().stream()
            .filter(a -> !a.isInline())
            .map(this::toAttachment)
            .toList();
        List<ZeptoMailRequest.Attachment> inlineImages = message.attachments().stream()
            .filter(MailAttachment::isInline)
            .map(this::toAttachment)
            .toList();

        return new ZeptoMailRequest(
            toAddress(message.from()),
            List.of(toRecipient(message.to())),
            message.cc().stream().map(this::toRecipient).toList(),
            message.bcc().stream().map(this::toRecipient).toList(),
            message.replyTo() != null ? List.of(toAddress(message.replyTo())) : null,
            message.subject(),
            message.htmlBody(),
            message.textBody(),
            attachments.isEmpty() ? null : attachments,
            inlineImages.isEmpty() ? null : inlineImages
        );
    }

    private ZeptoMailRequest.EmailAddress toAddress(MailAddress address) {
        return new ZeptoMailRequest.EmailAddress(address.email(), address.name());
    }

    private ZeptoMailRequest.Recipient toRecipient(MailAddress address) {
        return new ZeptoMailRequest.Recipient(toAddress(address));
    }

    private ZeptoMailRequest.Attachment toAttachment(MailAttachment attachment) {
        String base64 = Base64.getEncoder().encodeToString(attachment.content());
        return new ZeptoMailRequest.Attachment(base64, attachment.contentType(), attachment.filename(), attachment.contentId());
    }

    @Override
    public String providerId() {
        return "zeptomail";
    }
}
