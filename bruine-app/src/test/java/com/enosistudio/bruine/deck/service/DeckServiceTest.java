package com.enosistudio.bruine.deck.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Règles du deck vitrine.
 * <p>
 * Le deck est public : il est affiché sur le profil et partageable en image ailleurs. Ce
 * qu'il accepte doit donc appartenir au joueur, et lui appartenir encore.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(DeckService.class)
class DeckServiceTest {

    @Autowired
    private DeckService deckService;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private MarketListingRepository marketListingRepository;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    private SteamUser owner;
    private SteamUser stranger;
    private GachaReward dragon;

    @BeforeEach
    void setUp() {
        owner = createUser("76561190000000001");
        stranger = createUser("76561190000000002");
        dragon = createReward(ECardRarity.EPIC);
    }

    @Test
    void savingKeepsTheChosenCards() {
        Long first = createCard(owner).getId();
        Long second = createCard(owner).getId();

        deckService.saveDeck(owner.getId(), List.of(first, second));

        assertEquals(Set.of(first, second), deckService.findDeckCardIds(owner.getId()));
    }

    @Test
    void theDeckIsCreatedOnFirstSaveAndBelongsToItsOwner() {
        deckService.saveDeck(owner.getId(), List.of(createCard(owner).getId()));

        Deck deck = deckRepository.findBySteamUserId(owner.getId()).orElseThrow();
        assertEquals(owner.getId(), deck.getSteamUser().getId());
    }

    @Test
    void savingReplacesThePreviousContent() {
        Long before = createCard(owner).getId();
        Long after = createCard(owner).getId();
        deckService.saveDeck(owner.getId(), List.of(before));

        deckService.saveDeck(owner.getId(), List.of(after));

        assertEquals(Set.of(after), deckService.findDeckCardIds(owner.getId()));
    }

    @Test
    void savingWithoutAnyCardEmptiesTheDeck() {
        deckService.saveDeck(owner.getId(), List.of(createCard(owner).getId()));

        deckService.saveDeck(owner.getId(), null);

        assertTrue(deckService.findDeckCardIds(owner.getId()).isEmpty());
    }

    @Test
    void aCardBelongingToSomeoneElseIsRejected() {
        Long mine = createCard(owner).getId();
        Long theirs = createCard(stranger).getId();

        deckService.saveDeck(owner.getId(), List.of(mine, theirs));

        assertEquals(Set.of(mine), deckService.findDeckCardIds(owner.getId()));
    }

    /**
     * Une carte en vente peut changer de propriétaire : elle n'a rien à faire en vitrine.
     */
    @Test
    void aCardListedOnTheMarketIsRejected() {
        Long kept = createCard(owner).getId();
        UserCard onSale = createCard(owner);
        putOnSale(onSale);

        deckService.saveDeck(owner.getId(), List.of(kept, onSale.getId()));

        assertEquals(Set.of(kept), deckService.findDeckCardIds(owner.getId()));
    }

    @Test
    void anUnknownCardIsIgnored() {
        Long existing = createCard(owner).getId();

        deckService.saveDeck(owner.getId(), List.of(existing, 999999L));

        assertEquals(Set.of(existing), deckService.findDeckCardIds(owner.getId()));
    }

    @Test
    void theSameCardCannotBePlacedTwice() {
        Long card = createCard(owner).getId();

        deckService.saveDeck(owner.getId(), List.of(card, card));

        assertEquals(1, deckService.findDeckCards(owner.getId()).size());
    }

    @Test
    void theDeckHoldsTenCardsAtMost() {
        List<Long> twelve = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            twelve.add(createCard(owner).getId());
        }

        deckService.saveDeck(owner.getId(), twelve);

        assertEquals(10, deckService.findDeckCardIds(owner.getId()).size());
    }

    /**
     * MarketService interroge cette méthode avant chaque vente. Un joueur sans deck ne
     * doit pas s'y voir refuser une vente, l'absence de deck valant deck vide.
     */
    @Test
    void aPlayerWithoutADeckHoldsNoCard() {
        assertTrue(deckService.findDeckCardIds(owner.getId()).isEmpty());
        assertTrue(deckService.findDeckCards(owner.getId()).isEmpty());
    }

    private SteamUser createUser(String steamId) {
        SteamUser user = new SteamUser();
        user.setSteamId(steamId);
        user.setUsername("joueur " + steamId);
        return userRepository.save(user);
    }

    private GachaReward createReward(ECardRarity rarity) {
        GachaReward reward = new GachaReward();
        reward.setRarity(rarity);
        reward.setName("Carte " + rarity);
        reward.setEmoji("*");
        return gachaRewardRepository.save(reward);
    }

    private UserCard createCard(SteamUser holder) {
        UserCard card = new UserCard();
        card.setSteamUser(holder);
        card.setGachaReward(dragon);
        card.setFinish(ECardFinish.NORMAL);
        return userCardRepository.save(card);
    }

    private void putOnSale(UserCard card) {
        MarketListing listing = new MarketListing();
        listing.setSeller(card.getSteamUser());
        listing.setUserCard(card);
        listing.setPrice(10);
        marketListingRepository.save(listing);
    }
}
