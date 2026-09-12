package com.enosistudio.bruine.gacha;

import com.enosistudio.bruine.card.CardStackDTO;
import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.level.dto.ConvertibleCardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que l'inventaire, la page de conversion et l'éditeur de deck rendent leur
 * carte par le fragment partagé.
 * <p>
 * Le test n'écrit pas ses propres appels : il extrait l'appel réel de chaque page et
 * l'exécute. Une page qui reviendrait à du balisage recopié, ou dont les paramètres ne
 * correspondraient plus à la signature du fragment, fait donc échouer ce test.
 */
class CardFragmentTest {

    private static final String CALL_MARKER = "~{fragments/card :: card(";

    private SpringTemplateEngine engine;
    private GachaReward reward;

    @BeforeEach
    void setUp() {
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.refresh();

        SpringResourceTemplateResolver templates = new SpringResourceTemplateResolver();
        templates.setApplicationContext(ctx);
        templates.setPrefix("classpath:/templates/");
        templates.setSuffix(".html");
        templates.setTemplateMode(TemplateMode.HTML);
        templates.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Sans cette restriction, ce résolveur essaierait de charger la chaîne hôte
        // du test comme s'il s'agissait d'un chemin de fichier.
        templates.setResolvablePatterns(Set.of("fragments/*"));
        templates.setOrder(1);

        StringTemplateResolver host = new StringTemplateResolver();
        host.setTemplateMode(TemplateMode.HTML);
        host.setOrder(2);

        engine = new SpringTemplateEngine();
        engine.addTemplateResolver(templates);
        engine.addTemplateResolver(host);

        reward = new GachaReward();
        reward.setId(42L);
        reward.setRarity(ECardRarity.EPIC);
        reward.setEmoji("E");
        reward.setName("Carte de test");
        reward.setDescription("Texte de saveur");
    }

    /**
     * Extrait de la page l'élément qui délègue au fragment de carte.
     */
    private String cardCallFrom(String templatePath) throws Exception {
        List<String> lines;
        try (InputStream in = getClass().getResourceAsStream(templatePath)) {
            assertNotNull(in, "Page introuvable : " + templatePath);
            lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }

        int marker = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(CALL_MARKER)) {
                marker = i;
                break;
            }
        }
        assertTrue(marker >= 0, templatePath + " n'appelle plus le fragment de carte");

        // On extrait le seul élément qui délègue au fragment. La boucle qui l'entoure ne
        // fait pas partie du rendu d'une carte : le test fournit directement sa variable.
        int start = marker;
        while (start > 0 && !lines.get(start).contains("<div")) {
            start--;
        }
        int end = marker;
        while (end < lines.size() - 1 && !lines.get(end).contains("</div>")) {
            end++;
        }
        return "<div xmlns:th=\"http://www.thymeleaf.org\">"
                + String.join("\n", lines.subList(start, end + 1))
                + "</div>";
    }

    private String render(String templatePath, Map<String, Object> variables) throws Exception {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return engine.process(cardCallFrom(templatePath), context);
    }

    /**
     * Structure commune attendue quelle que soit la page appelante.
     */
    private void assertCommonCardStructure(String out, String baseClass) {
        assertTrue(out.contains(baseClass), out);
        assertTrue(out.contains("epic"), out);
        assertTrue(out.contains("finish-holographic"), out);
        assertTrue(out.contains("class=\"card-header\""), out);
        assertTrue(out.contains("class=\"card-artwork\""), out);
        assertTrue(out.contains("class=\"card-name-strip\""), out);
        assertTrue(out.contains("class=\"card-footer\""), out);
        assertTrue(out.contains("Carte de test"), out);
        assertTrue(out.contains("HOLO"), out);
        assertFalse(out.contains("th:"), "Fragment non résolu : " + out);
    }

    @Test
    void inventoryPageUsesSharedFragment() throws Exception {
        CardStackDTO stack = new CardStackDTO(reward, ECardFinish.HOLOGRAPHIC, List.of(1L, 2L, 3L));
        String out = render("/templates/inventory/inventory.html", Map.of("item", stack));

        assertCommonCardStructure(out, "inv-card");
        assertTrue(out.contains("Texte de saveur"), "L'inventaire affiche la description : " + out);
        assertTrue(out.contains("card-count"), "L'inventaire compte au delà d'un exemplaire : " + out);
        assertTrue(out.contains("card-shine"), "Le reflet porte le mouvement au survol : " + out);
    }

    @Test
    void inventoryHidesCounterForASingleCopy() throws Exception {
        CardStackDTO single = new CardStackDTO(reward, ECardFinish.HOLOGRAPHIC, List.of(1L));
        String out = render("/templates/inventory/inventory.html", Map.of("item", single));

        assertFalse(out.contains("card-count"), "Un exemplaire unique n'affiche pas de compteur : " + out);
    }

    @Test
    void levelUpPageUsesSharedFragment() throws Exception {
        ConvertibleCardDTO card = new ConvertibleCardDTO(reward, ECardFinish.HOLOGRAPHIC, 2, 400);
        String out = render("/templates/levelup/levelup.html", Map.of("item", card));

        assertCommonCardStructure(out, "inv-card");
        assertTrue(out.contains("Texte de saveur"), "La carte est identique partout, description comprise : " + out);
        assertTrue(out.contains("card-count"), "Le level up affiche toujours le compteur : " + out);
        assertTrue(out.contains("card-shine"), "Le reflet porte le mouvement au survol : " + out);
    }

    @Test
    void deckEditorUsesSharedFragment() throws Exception {
        CardStackDTO stack = new CardStackDTO(reward, ECardFinish.HOLOGRAPHIC, List.of(7L, 8L, 9L));
        String out = render("/templates/deck/deck.html", Map.of("stack", stack));

        assertCommonCardStructure(out, "balatro-card");
        assertTrue(out.contains("data-id=\"7,8,9\""),
                "L'éditeur détache un exemplaire précis, il lui faut toute la réserve : " + out);
        assertTrue(out.contains("Texte de saveur"),
                "L'éditeur affiche la même carte que l'inventaire, description comprise : " + out);
        assertTrue(out.contains("card-count"), "Les exemplaires identiques sont empilés : " + out);
        assertTrue(out.contains("card-shine"), "Le reflet porte le mouvement au survol : " + out);
    }
}
