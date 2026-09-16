package com.enosistudio.bruine.market;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.repository.UserCardRepository;
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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@Transactional
class MarketControllerTest {

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

        session = new MockHttpSession();
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(
                SteamAuthenticationToken.authenticated(SteamUserPrincipal.create(player, null))));
    }

    @Test
    void theSellFormListsEachFreeCardOnceWithItsFinishAndCount() throws Exception {
        UserCard first = createCard(ECardFinish.NORMAL);
        UserCard second = createCard(ECardFinish.NORMAL);
        UserCard foil = createCard(ECardFinish.FOIL);

        mvc.perform(get("/market").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<option value=\"" + first.getId() + "\">* Pierre Grise · Commun ×2</option>"),
                        not(containsString("<option value=\"" + second.getId() + "\"")),
                        containsString("<option value=\"" + foil.getId() + "\">* Pierre Grise "
                                + ECardFinish.FOIL.getEmoji() + " · Commun</option>"))));
    }

    private UserCard createCard(ECardFinish finish) {
        UserCard card = new UserCard();
        card.setSteamUser(player);
        card.setGachaReward(reward);
        card.setFinish(finish);
        return userCardRepository.save(card);
    }
}
