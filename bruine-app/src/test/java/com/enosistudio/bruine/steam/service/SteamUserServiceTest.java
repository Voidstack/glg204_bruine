package com.enosistudio.bruine.steam.service;

import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@Import(SteamUserService.class)
class SteamUserServiceTest {

    private static final String STEAM_ID = "76561190000000001";

    @Autowired
    private SteamUserService steamUserService;

    @Autowired
    private DeckRepository deckRepository;

    @Test
    void aNewPlayerGetsAnEmptyDeck() {
        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 120L);

        Deck deck = deckRepository.findBySteamUserId(player.getId()).orElseThrow();
        assertTrue(deck.getCards().isEmpty());
    }

    @Test
    void aReturningPlayerKeepsHisSingleDeck() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 120L);

        steamUserService.recordLogin(STEAM_ID, "Joueuse", 180L);

        assertEquals(1, deckRepository.count());
    }
}
