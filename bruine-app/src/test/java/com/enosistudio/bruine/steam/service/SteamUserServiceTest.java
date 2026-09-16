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

    @Test
    void aNewPlayerEarnsNothingForThePlaytimeHeAlreadyHad() {
        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 5000L);

        assertEquals(0, player.getScore());
        assertEquals(5000L, player.getInitialPlaytimeMinutes());
    }

    @Test
    void eachFullHourPlayedSinceTheLastLoginPaysOnePull() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 120L);

        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 300L);

        assertEquals(3 * SteamUserService.POINTS_PER_PLAYED_HOUR, player.getScore());
        assertEquals(300L, player.getCurrentPlaytimeMinutes());
    }

    @Test
    void minutesBelowAFullHourPayNothing() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 60L);

        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 119L);

        assertEquals(0, player.getScore());
    }

    @Test
    void anHourCompletedAcrossTwoLoginsIsCredited() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 90L);
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 110L);

        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 130L);

        assertEquals(SteamUserService.POINTS_PER_PLAYED_HOUR, player.getScore());
    }

    @Test
    void theSameHoursAreNeverCreditedTwice() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 120L);
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 300L);

        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 300L);

        assertEquals(3 * SteamUserService.POINTS_PER_PLAYED_HOUR, player.getScore());
    }

    @Test
    void aDecreasingPlaytimeCreditsNothingAndKeepsTheCounter() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 300L);
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 60L);

        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 360L);

        assertEquals(SteamUserService.POINTS_PER_PLAYED_HOUR, player.getScore());
        assertEquals(360L, player.getCurrentPlaytimeMinutes());
    }

    @Test
    void aPrivateProfileLeavesTheCountersUntouched() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", 120L);

        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", null);

        assertEquals(0, player.getScore());
        assertEquals(120L, player.getCurrentPlaytimeMinutes());
    }

    @Test
    void aProfileThatWasPrivateAtSignUpStartsCountingWhenItOpens() {
        steamUserService.recordLogin(STEAM_ID, "Joueuse", null);

        SteamUser player = steamUserService.recordLogin(STEAM_ID, "Joueuse", 5000L);

        assertEquals(0, player.getScore());
        assertEquals(5000L, player.getCurrentPlaytimeMinutes());
    }
}
