package org.cthub.backend.mail.render;

import org.cthub.backend.mail.config.MailAutoConfig;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders every template in both supported locales and asserts there are no unresolved message
 * keys (Spring's {@code ResourceBundleMessageSource} with {@code useCodeAsDefaultMessage(true)}
 * falls back to printing the raw key when a translation is missing, which we treat as a bug).
 */
class ThymeleafMailRendererTest {

    private static final Pattern UNRESOLVED_KEY = Pattern.compile("mail\\.[a-zA-Z.]+");
    private static final List<Locale> LOCALES = List.of(Locale.GERMAN, Locale.ENGLISH);

    private ThymeleafMailRenderer renderer;

    @BeforeEach
    void setUp() {
        // SpringResourceTemplateResolver is ApplicationContextAware and needs a real context to
        // resolve "classpath:" resources - a plain GenericApplicationContext is enough, no beans needed.
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.refresh();

        MailAutoConfig autoConfig = new MailAutoConfig();
        ResourceBundleMessageSource messageSource = autoConfig.mailMessageSource();
        ITemplateResolver htmlResolver = autoConfig.mailTemplateResolver();
        ITemplateResolver textResolver = autoConfig.mailTextTemplateResolver();
        ((org.springframework.context.ApplicationContextAware) htmlResolver).setApplicationContext(applicationContext);
        ((org.springframework.context.ApplicationContextAware) textResolver).setApplicationContext(applicationContext);
        SpringTemplateEngine engine = autoConfig.mailTemplateEngine(htmlResolver, textResolver);

        MailProperties properties = new MailProperties();
        properties.setBaseUrl("https://example.org");
        renderer = new ThymeleafMailRenderer(engine, messageSource, properties);
    }

    @Test
    void rendersRegistrationConfirmationInGermanAndEnglish() {
        RegistrationConfirmationModel model =
            new RegistrationConfirmationModel("Maria", "https://example.org/verify-email?token=abc", Duration.ofHours(24));

        for (Locale locale : LOCALES) {
            MailMessage message = renderer.render(
                MailTemplate.REGISTRATION_CONFIRMATION, model, locale, MailAddress.of("maria@example.org"));

            assertThat(message.subject()).isNotBlank();
            assertThat(UNRESOLVED_KEY.matcher(message.subject()).find()).isFalse();
            assertThat(message.htmlBody()).contains("Maria").contains(model.confirmUrl());
            assertThat(message.textBody()).contains("Maria").contains(model.confirmUrl());
            assertThat(message.htmlBody()).doesNotContain("??");
            assertThat(message.textBody()).doesNotContain("??");
        }
    }

    @Test
    void rendersWelcomeAndPasswordReset() {
        assertRendersCleanly(MailTemplate.WELCOME, new WelcomeModel("Tom"));
        assertRendersCleanly(MailTemplate.PASSWORD_RESET,
            new PasswordResetModel("Tom", "https://example.org/reset-password?token=xyz", Duration.ofHours(1)));
        assertRendersCleanly(MailTemplate.LOGIN_LINK,
            new LoginLinkModel("Tom", "https://example.org/login-link?token=xyz", Duration.ofMinutes(15)));
    }

    /**
     * Registration only collects an e-mail address, so mail routinely goes out before a name
     * exists. Those renders must fall back to an anonymous greeting rather than printing "null".
     */
    @Test
    void rendersAnonymousGreetingWhenFirstNameIsMissing() {
        List<MailTemplateModel> namelessModels = List.of(
            new RegistrationConfirmationModel(null, "https://example.org/verify-email?token=abc", Duration.ofHours(24)),
            new PasswordResetModel(null, "https://example.org/reset-password?token=xyz", Duration.ofHours(1)),
            new LoginLinkModel(null, "https://example.org/login-link?token=xyz", Duration.ofMinutes(15)),
            new WelcomeModel(null));
        List<MailTemplate> templates = List.of(
            MailTemplate.REGISTRATION_CONFIRMATION,
            MailTemplate.PASSWORD_RESET,
            MailTemplate.LOGIN_LINK,
            MailTemplate.WELCOME);

        for (int i = 0; i < templates.size(); i++) {
            for (Locale locale : LOCALES) {
                MailMessage message = renderer.render(
                    templates.get(i), namelessModels.get(i), locale, MailAddress.of("test@example.org"));

                assertThat(message.htmlBody()).doesNotContain("null").doesNotContain("{0}");
                assertThat(message.textBody()).doesNotContain("null").doesNotContain("{0}");
                assertThat(UNRESOLVED_KEY.matcher(message.textBody()).find()).isFalse();
            }
        }
    }

    /**
     * The logo and the legal links are absolute URLs built from the configured base URL - relative
     * paths are meaningless inside a mail client.
     */
    @Test
    void rendersTheBrandedFooterWithAbsoluteUrls() {
        MailMessage message = renderer.render(
            MailTemplate.WELCOME, new WelcomeModel("Tom"), Locale.ENGLISH, MailAddress.of("tom@example.org"));

        assertThat(message.htmlBody())
            .contains("https://example.org/icon-192.png")
            .contains("https://example.org/impressum")
            .contains("https://example.org/privacy")
            .contains("Sebastian Schreitter")
            .contains("LEGO");
        assertThat(message.textBody())
            .contains("https://example.org/impressum")
            .contains("Sebastian Schreitter");
        // MessageFormat would render a raw int as "2,026" / "2.026" depending on locale.
        assertThat(message.htmlBody()).containsPattern("\u00a9 \\d{4} by");
    }

    /**
     * Notification texts are user-supplied (e.g. an admin announcement): they become the subject
     * verbatim, keep their paragraph structure, and are always escaped in the HTML part.
     */
    @Test
    void rendersNotificationWithSubjectParagraphsAndEscaping() {
        NotificationMailModel model = new NotificationMailModel(
            "Tom", "Season's <b>kick-off</b>", "First line\nsecond line\n\n<script>alert(1)</script>",
            "https://example.org/competition/2025", "Announcements");

        assertRendersCleanly(MailTemplate.NOTIFICATION, model);
        for (Locale locale : LOCALES) {
            MailMessage message = renderer.render(MailTemplate.NOTIFICATION, model, locale, MailAddress.of("tom@example.org"));

            assertThat(message.subject()).isEqualTo("Season's <b>kick-off</b>");
            assertThat(message.htmlBody())
                .contains("Season&#39;s &lt;b&gt;kick-off&lt;/b&gt;")
                .containsPattern("First line<br\\s*/?>second line")
                .contains("&lt;script&gt;")
                .doesNotContain("<script>")
                .contains("https://example.org/competition/2025")
                .contains("https://example.org/account/notifications/settings")
                .contains("Announcements");
            assertThat(message.textBody())
                .contains("First line\nsecond line")
                .contains("https://example.org/competition/2025")
                .contains("https://example.org/account/notifications/settings");
            assertThat(UNRESOLVED_KEY.matcher(message.htmlBody()).find()).isFalse();
            assertThat(UNRESOLVED_KEY.matcher(message.textBody()).find()).isFalse();
        }
    }

    @Test
    void notificationTitleIsFlattenedToASingleLine() {
        NotificationMailModel model = new NotificationMailModel(null, " Line one\r\n  line two ", "", "https://example.org", "x");

        assertThat(model.title()).isEqualTo("Line one line two");
        assertThat(model.paragraphs()).isEmpty();
    }

    private void assertRendersCleanly(MailTemplate template, MailTemplateModel model) {
        for (Locale locale : LOCALES) {
            MailMessage message = renderer.render(template, model, locale, MailAddress.of("test@example.org"));
            assertThat(message.subject()).isNotBlank();
            assertThat(UNRESOLVED_KEY.matcher(message.subject()).find()).isFalse();
            assertThat(message.htmlBody()).doesNotContain("??");
            assertThat(message.textBody()).doesNotContain("??");
        }
    }
}
