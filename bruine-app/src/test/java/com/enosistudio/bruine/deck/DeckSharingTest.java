package com.enosistudio.bruine.deck;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import com.enosistudio.bruine.steam.service.SteamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@Transactional
class DeckSharingTest {

    private static final String STEAM_ID = "76561190000000009";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SteamUserRepository steamUserRepository;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private DeckService deckService;

    @Autowired
    private DeckRepository deckRepository;

    @MockitoBean
    private SteamService steamService;

    @BeforeEach
    void setUp() throws Exception {
        SteamUser user = new SteamUser();
        user.setSteamId(STEAM_ID);
        user.setUsername("Joueuse");
        user.setScore(0);
        user = steamUserRepository.save(user);
        deckRepository.save(new Deck(user));

        GachaReward reward = new GachaReward();
        reward.setRarity(ECardRarity.EPIC);
        reward.setName("Dragon de brume");
        reward.setEmoji("*");
        gachaRewardRepository.save(reward);

        UserCard card = new UserCard();
        card.setSteamUser(user);
        card.setGachaReward(reward);
        card.setFinish(ECardFinish.HOLOGRAPHIC);
        card = userCardRepository.save(card);

        deckService.saveDeck(user.getId(), List.of(card.getId()));

        when(steamService.getUserData(anyString())).thenReturn(Map.of(
                "steamid", STEAM_ID,
                "personaname", "Joueuse",
                "avatarfull", "",
                "avatarmedium", "",
                "personastate", 1,
                "communityvisibilitystate", 3));
    }

    @Test
    void theDeckIsServedAsAnSvgImage() throws Exception {
        mvc.perform(get("/deck/" + STEAM_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
    }

    @Test
    void theDeckTemplateIsAValidSvgFile() throws Exception {
        String svg;
        try (InputStream in = getClass().getResourceAsStream("/templates/deck/svg/deck.svg")) {
            assertNotNull(in, "templates/deck/svg/deck.svg introuvable");
            svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));

        assertEquals("svg", doc.getDocumentElement().getLocalName());
        assertTrue(doc.getDocumentElement().hasAttribute("viewBox"), "viewBox manquant");
        assertTrue(svg.contains("xmlns:th="), "namespace th non déclaré");
    }

    @Test
    void theProfileShowsTheDeckImageWithoutAnIframe() throws Exception {
        String html = mvc.perform(get("/steam/profile/" + STEAM_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("/deck/" + STEAM_ID), "l'image du deck doit pointer sur /deck/{steamId}");
        assertFalse(html.contains("<iframe"), "plus aucune iframe : l'ancien widget HTML est supprimé");
    }
}
