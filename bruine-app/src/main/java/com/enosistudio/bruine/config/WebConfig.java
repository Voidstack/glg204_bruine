package com.enosistudio.bruine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Réglages MVC de l'application.
 */
@Configuration
public class WebConfig {

    /**
     * L'interface est en français. On suit l'en-tête Accept-Language du navigateur,
     * en retombant sur le français quand il ne demande rien de connu.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.FRANCE);
        return resolver;
    }
}
