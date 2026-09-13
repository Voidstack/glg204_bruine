package com.enosistudio.bruine.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.GenericApplicationContext;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ThymeleafSvgConfigTest {

    private static final String SVG_DIR = "/templates/levelup/svg/";
    private static final Pattern URL_REF = Pattern.compile("url\\(#([\\w-]+)\\)");
    private static final Pattern ID_DEF = Pattern.compile("id=\"([\\w-]+)\"");

    private SpringTemplateEngine engine;

    @BeforeEach
    void setUp() {
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.refresh();

        StringTemplateResolver hostResolver = new StringTemplateResolver();
        hostResolver.setTemplateMode(TemplateMode.HTML);
        hostResolver.setOrder(ThymeleafSvgConfig.SVG_RESOLVER_ORDER + 1);

        engine = new SpringTemplateEngine();
        engine.addTemplateResolver(new ThymeleafSvgConfig().svgTemplateResolver(ctx));
        engine.addTemplateResolver(hostResolver);
    }

    @ParameterizedTest
    @ValueSource(strings = {"chassis", "gauge", "gear-large", "gear-small"})
    void eachLayerIsAStandaloneSvg(String layer) throws Exception {
        String svg = read(SVG_DIR + layer + ".svg");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));
        assertEquals("svg", doc.getDocumentElement().getLocalName(), layer + " : racine attendue <svg>");
        assertEquals("http://www.w3.org/2000/svg", doc.getDocumentElement().getNamespaceURI(), layer);
        assertTrue(doc.getDocumentElement().hasAttribute("viewBox"), layer + " : viewBox manquant");

        assertFalse(svg.contains("th:"), layer + " : le fichier doit rester du SVG pur");
        assertFalse(svg.contains("thymeleaf"), layer + " : namespace Thymeleaf interdit dans un calque");

        Set<String> ids = new HashSet<>();
        Matcher defs = ID_DEF.matcher(svg);
        while (defs.find()) ids.add(defs.group(1));
        Matcher refs = URL_REF.matcher(svg);
        while (refs.find()) {
            assertTrue(ids.contains(refs.group(1)),
                    layer + " : url(#" + refs.group(1) + ") pointe hors du fichier, le rendu isolé serait faux");
        }
    }

    @Test
    void theLevelUpPageInlinesEveryLayer() throws Exception {
        String out = engine.process(machineBlockFromRealPage(), new Context());

        assertFalse(out.contains("th:"), "Attributs Thymeleaf non résolus : " + out);
        assertFalse(out.contains("<?xml"), "Prologue XML inliné dans la page : " + out);

        assertEquals(6, out.split("<svg").length - 1, out);

        assertTrue(out.contains("class=\"anim-gear-large\""), out);
        assertTrue(out.contains("class=\"gauge-needle-rotate\""), out);
        assertTrue(out.contains("class=\"gear-core-glow\""), out);
        assertEquals(2, out.split("class=\"anim-gear-small\"").length - 1, out);

        assertTrue(out.contains("id=\"wstripes\""), out);
        assertTrue(out.contains("id=\"coreGlow\""), out);
        assertTrue(out.contains("fill=\"url(#wstripes)\""), out);
        assertTrue(out.contains("fill=\"url(#coreGlow)\""), out);

        long wrappers = out.lines().filter(line -> line.contains("<g transform=\"translate(")).count();
        assertEquals(4, wrappers, out);
    }

    @Test
    void htmlModePreservesSvgAttributeCase() throws Exception {
        String out = engine.process(machineBlockFromRealPage(), new Context());

        assertTrue(out.contains("viewBox="), out);
        assertTrue(out.contains("patternTransform="), out);
        assertTrue(out.contains("patternUnits="), out);
        assertTrue(out.contains("<radialGradient"), out);
        assertTrue(out.contains("stop-opacity="), out);
        assertFalse(out.contains("viewbox="), "viewBox a été mis en minuscules");
        assertFalse(out.contains("patterntransform="), "patternTransform a été mis en minuscules");
    }

    private String machineBlockFromRealPage() throws Exception {
        String page = read("/templates/levelup/levelup.html");
        int start = page.indexOf("<svg class=\"machine-svg\"");
        int end = page.indexOf("</svg>", start) + "</svg>".length();
        assertTrue(start > 0 && end > start, "Bloc machine-svg introuvable dans levelup.html");
        return "<div xmlns:th=\"http://www.thymeleaf.org\">" + page.substring(start, end) + "</div>";
    }

    private static String read(String classpathPath) throws Exception {
        try (InputStream in = ThymeleafSvgConfigTest.class.getResourceAsStream(classpathPath)) {
            assertNotNull(in, "Ressource introuvable : " + classpathPath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
