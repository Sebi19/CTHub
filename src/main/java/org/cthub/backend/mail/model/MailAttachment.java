package org.cthub.backend.mail.model;

/**
 * A mail attachment. When {@code contentId} is set, the attachment is inlined and can be referenced
 * from the HTML body via {@code cid:<contentId>} (e.g. for an embedded logo).
 */
public record MailAttachment(String filename, String contentType, byte[] content, String contentId) {

    public static MailAttachment of(String filename, String contentType, byte[] content) {
        return new MailAttachment(filename, contentType, content, null);
    }

    public static MailAttachment inline(String contentId, String contentType, byte[] content) {
        return new MailAttachment(contentId, contentType, content, contentId);
    }

    public boolean isInline() {
        return contentId != null && !contentId.isBlank();
    }
}
