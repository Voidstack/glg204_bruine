package com.enosistudio.bruine.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * les fichiers .svg rangés dans templates.*.svg., c'est aussi des "templates"
 * de base templeaf n'ouvre que les fichiers .html, donc il faut un résolveur spécifique pour les .svg
 * <p>
 * j'utilise pour deux choses :
 * Générer une image SVG -> /deck/{deckId}/image.svg
 * Insérer un SVG dans un htlm <th:block th:replace="~{levelup/svg/chassis :: svg}"></th:block> ca me permet de les animer
 */
@Configuration
public class ThymeleafSvgConfig {

    /**
     * Ordre du résolveur SVG ; le résolveur HTML par défaut est placé après.
     */
    public static final int SVG_RESOLVER_ORDER = 1;

    /**
     * Chemins acceptés, relatifs à {@code templates/} : un dossier {@code svg/} imbriqué.
     */
    public static final String RESOLVABLE_PATTERN = "*/svg/*";

    @Bean
    public SpringResourceTemplateResolver svgTemplateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".svg");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setResolvablePatterns(Set.of(RESOLVABLE_PATTERN));
        resolver.setCheckExistence(true);
        resolver.setOrder(SVG_RESOLVER_ORDER);
        return resolver;
    }
}
