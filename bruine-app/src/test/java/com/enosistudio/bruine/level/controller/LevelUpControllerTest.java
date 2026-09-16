package com.enosistudio.bruine.level.controller;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.card.UserCardRepository;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.security.SteamUserPrincipal;
import com.enosistudio.bruine.steam.service.SteamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@Transactional
class LevelUpControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SteamUserRepository steamUserRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @MockitoBean
    private SteamService steamService;

    private SteamUser player;

    private GachaReward reward;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        player = new SteamUser();
        player.setSteamId("76561190000000001");
        player.setUsername("Joueur");
        steamUserRepository.save(player);
        deckRepository.save(new Deck(player));

        reward = new GachaReward();
        reward.setRarity(ECardRarity.COMMON);
        reward.setName("Pierre Grise");
        reward.setEmoji("*");
        gachaRewardRepository.save(reward);

        UserCard card = new UserCard();
        card.setSteamUser(player);
        card.setGachaReward(reward);
        card.setFinish(ECardFinish.FOIL);
        userCardRepository.save(card);

        session = new MockHttpSession();
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(
                SteamAuthenticationToken.authenticated(SteamUserPrincipal.create(player, null))));
    }

    @Test
    void thePageSendsTheFinishByItsConstantName() throws Exception {
        mvc.perform(get("/levelup").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-finish=\"FOIL\"")));
    }

    @Test
    void aFinishSentByItsConstantNameIsConverted() throws Exception {
        mvc.perform(post("/levelup/convert").session(session).with(csrf())
                        .param("items[0].rewardId", reward.getId().toString())
                        .param("items[0].finish", "FOIL"))
                .andExpect(redirectedUrl("/levelup"));

        assertEquals(0, userCardRepository.count());
        assertEquals(30, steamUserRepository.findById(player.getId()).orElseThrow().getTotalExperience());
    }

    @Test
    void anUnknownFinishIsRefusedWithoutConvertingAnything() throws Exception {
        mvc.perform(post("/levelup/convert").session(session).with(csrf())
                        .param("items[0].rewardId", reward.getId().toString())
                        .param("items[0].finish", "PLASMA"))
                .andExpect(status().isBadRequest());

        assertEquals(1, userCardRepository.count());
    }
}
