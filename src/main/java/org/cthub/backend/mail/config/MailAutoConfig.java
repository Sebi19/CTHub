package org.cthub.backend.mail.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;

/**
 * Wires a dedicated Thymeleaf {@link SpringTemplateEngine} for mail rendering, rooted at
 * {@code classpath:/mail/templates/}.
 *
 * <p><b>Important:</b> do not replace this with {@code spring-boot-starter-thymeleaf} auto-config.
 * That auto-config registers a view resolver on {@code classpath:/templates/} plus an {@code error}
 * view, which can intercept {@link org.cthub.backend.config.SpaConfig}'s 404-to-index.html forwarding
 * for the React SPA. Keeping the engine manual and template-mode HTML (not a Spring MVC view) avoids
 * that entirely.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailAutoConfig {

    @Bean
    public ITemplateResolver mailTemplateResolver() {
        // Template names passed to the engine include their extension (e.g. "registration-confirmation.html"),
        // so each resolver only ever handles its own file type via resolvablePatterns - no ambiguity.
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/mail/templates/");
        resolver.setSuffix("");
        resolver.setResolvablePatterns(java.util.Set.of("*.html"));
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        resolver.setOrder(1);
        return resolver;
    }

    @Bean
    public ITemplateResolver mailTextTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/mail/templates/");
        resolver.setSuffix("");
        resolver.setResolvablePatterns(java.util.Set.of("*.txt"));
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        resolver.setOrder(2);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine mailTemplateEngine(
        ITemplateResolver mailTemplateResolver,
        ITemplateResolver mailTextTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolvers(java.util.Set.of(mailTemplateResolver, mailTextTemplateResolver));
        engine.setTemplateEngineMessageSource(mailMessageSource());
        return engine;
    }

    @Bean
    public org.springframework.context.support.ResourceBundleMessageSource mailMessageSource() {
        org.springframework.context.support.ResourceBundleMessageSource messageSource =
            new org.springframework.context.support.ResourceBundleMessageSource();
        messageSource.setBasename("mail/i18n/mail");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        // German is the fallback language, matching the frontend convention.
        messageSource.setDefaultLocale(java.util.Locale.GERMAN);
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }
}
