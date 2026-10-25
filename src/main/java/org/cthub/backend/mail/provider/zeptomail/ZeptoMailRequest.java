package org.cthub.backend.mail.provider.zeptomail;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for ZeptoMail's plain-HTML "send mail" REST API
 * (https://api.zeptomail.eu/v1.1/email). We deliberately talk to this endpoint with the built-in
 * {@code RestClient} instead of pulling in the vendor SDK.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZeptoMailRequest(
    EmailAddress from,
    List<Recipient> to,
    List<Recipient> cc,
    List<Recipient> bcc,
    @JsonProperty("reply_to") List<EmailAddress> replyTo,
    String subject,
    String htmlbody,
    String textbody,
    List<Attachment> attachments,
    @JsonProperty("inline_images") List<Attachment> inlineImages
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EmailAddress(String address, String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Recipient(@JsonProperty("email_address") EmailAddress emailAddress) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Attachment(
        String content,
        @JsonProperty("mime_type") String mimeType,
        String name,
        String cid
    ) {
    }
}
