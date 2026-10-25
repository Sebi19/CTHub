package org.cthub.backend.mail.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MailPropertiesTest {

    @Test
    void allowsEverythingWhenNoAllowlistIsConfigured() {
        MailProperties properties = new MailProperties();

        assertThat(properties.isRecipientAllowed("anyone@example.org")).isTrue();
    }

    @Test
    void matchesFullAddressesCaseInsensitively() {
        MailProperties properties = new MailProperties();
        properties.setAllowedRecipients(List.of("Me@Example.org"));

        assertThat(properties.isRecipientAllowed("me@example.org")).isTrue();
        assertThat(properties.isRecipientAllowed("  ME@EXAMPLE.ORG ")).isTrue();
        assertThat(properties.isRecipientAllowed("someone.else@example.org")).isFalse();
    }

    @Test
    void matchesWholeDomainsWithAndWithoutLeadingAt() {
        MailProperties properties = new MailProperties();
        properties.setAllowedRecipients(List.of("@challengeteamhub.org", "example.org"));

        assertThat(properties.isRecipientAllowed("anyone@challengeteamhub.org")).isTrue();
        assertThat(properties.isRecipientAllowed("anyone@example.org")).isTrue();
        assertThat(properties.isRecipientAllowed("anyone@evil.org")).isFalse();
        // Must not match a domain that merely ends with an allowed one.
        assertThat(properties.isRecipientAllowed("anyone@notexample.org")).isFalse();
    }

    @Test
    void rejectsBlankRecipientsWhenAnAllowlistIsConfigured() {
        MailProperties properties = new MailProperties();
        properties.setAllowedRecipients(List.of("me@example.org"));

        assertThat(properties.isRecipientAllowed(null)).isFalse();
        assertThat(properties.isRecipientAllowed("  ")).isFalse();
    }
}
