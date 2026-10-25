package org.cthub.backend.mail.render;

import lombok.RequiredArgsConstructor;
import org.cthub.backend.mail.config.MailProperties;
import org.cthub.backend.mail.model.MailAddress;
import org.cthub.backend.mail.model.MailMessage;
import org.cthub.backend.mail.model.MailTemplate;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders {@link MailTemplate}s with the dedicated mail {@link SpringTemplateEngine} (see
 * {@link org.cthub.backend.mail.config.MailAutoConfig}). Rendering happens eagerly at enqueue time
 * (not at send time), so a template bug surfaces in the request and retries can't diverge in content.
 */
@Component
@RequiredArgsConstructor
public class ThymeleafMailRenderer implements MailRenderer {

    private final SpringTemplateEngine mailTemplateEngine;
    private final MessageSource mailMessageSource;
    private final MailProperties mailProperties;

    @Override
    public MailMessage render(MailTemplate template, MailTemplateModel model, Locale locale, MailAddress recipient) {
        Context context = new Context(locale, toTemplateVariables(template, model));

        String html = mailTemplateEngine.process(template.templatePath() + ".html", context);
        String text = mailTemplateEngine.process(template.templatePath() + ".txt", context);
        String subject = mailMessageSource.getMessage(template.subjectKey(), null, locale);

        return new MailMessage(null, recipient, null, null, null, subject, html, text, null, null,
            List.of(template.name()));
    }

    /**
     * Note the deliberate use of {@link HashMap} over {@code Map.of}: registration is e-mail-only,
     * so {@code firstName} is frequently {@code null} and {@code Map.of} rejects null values.
     */
    private Map<String, Object> toTemplateVariables(MailTemplate template, MailTemplateModel model) {
        Map<String, Object> variables = new HashMap<>();
        // Available to every template: the layout needs them for the logo, the legal links and the
        // copyright line, and no individual model should have to carry them.
        variables.put("baseUrl", mailProperties.getBaseUrl());
        variables.put("currentYear", String.valueOf(Year.now().getValue()));

        switch (model) {
            case RegistrationConfirmationModel m -> {
                variables.put("firstName", m.firstName());
                variables.put("confirmUrl", m.confirmUrl());
                variables.put("validityHours", m.validity().toHours());
            }
            case WelcomeModel m -> variables.put("firstName", m.firstName());
            case PasswordResetModel m -> {
                variables.put("firstName", m.firstName());
                variables.put("resetUrl", m.resetUrl());
                variables.put("validityMinutes", m.validity().toMinutes());
            }
            case LoginLinkModel m -> {
                variables.put("firstName", m.firstName());
                variables.put("loginUrl", m.loginUrl());
                variables.put("validityMinutes", m.validity().toMinutes());
            }
            default -> throw new IllegalArgumentException(
                "Unsupported mail template model " + model.getClass().getSimpleName() + " for template " + template);
        }
        return variables;
    }
}
