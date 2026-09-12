package com.enosistudio.bruine.config;

import com.enosistudio.bruine.common.DateFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final DateFormatter dateFormatter;

    public WebConfig(@Autowired DateFormatter dateFormatter) {
        this.dateFormatter = dateFormatter;
    }

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addFormatter(dateFormatter);
    }


    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.FRANCE);
        return resolver;
    }


}
