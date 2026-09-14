package com.enosistudio.bruine.deck.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.card.UserCardService;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@Import({DeckService.class, UserCardService.class})
class DeckServiceTest {

    @Autowired
    private DeckService deckService;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private SteamUserRepository steamUserRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private MarketListingRepository marketListingRepository;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    @Autowired
    private TestEntityManager entityManager;

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

        assertEquals(List.of(first, second), deckService.findDeckCardIds(owner.getId()));
    }

    @Test
    void theDeckKeepsTheOrderChosenByThePlayer() {
        Long first = createCard(owner).getId();
        Long second = createCard(owner).getId();
        Long third = createCard(owner).getId();
        deckService.saveDeck(owner.getId(), List.of(first, second, third));
        entityManager.flush();
        entityManager.clear();

        deckService.saveDeck(owner.getId(), List.of(third, first, second));
        entityManager.flush();
        entityManager.clear();

        assertEquals(List.of(third, first, second),
                deckService.findDeckCards(owner.getId()).stream().map(UserCard::getId).toList());
    }

    @Test
    void savingReplacesThePreviousContent() {
        Long before = createCard(owner).getId();
        Long after = createCard(owner).getId();
        deckService.saveDeck(owner.getId(), List.of(before));

        deckService.saveDeck(owner.getId(), List.of(after));

        assertEquals(List.of(after), deckService.findDeckCardIds(owner.getId()));
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

        assertEquals(List.of(mine), deckService.findDeckCardIds(owner.getId()));
    }

    @Test
    void aCardListedOnTheMarketIsRejected() {
        Long kept = createCard(owner).getId();
        UserCard onSale = createCard(owner);
        putOnSale(onSale);

        deckService.saveDeck(owner.getId(), List.of(kept, onSale.getId()));

        assertEquals(List.of(kept), deckService.findDeckCardIds(owner.getId()));
    }

    @Test
    void anUnknownCardIsIgnored() {
        Long existing = createCard(owner).getId();

        deckService.saveDeck(owner.getId(), List.of(existing, 999999L));

        assertEquals(List.of(existing), deckService.findDeckCardIds(owner.getId()));
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

    @Test
    void aNewDeckHoldsNoCard() {
        assertTrue(deckService.findDeckCardIds(owner.getId()).isEmpty());
        assertTrue(deckService.findDeckCards(owner.getId()).isEmpty());
    }

    private SteamUser createUser(String steamId) {
        SteamUser user = new SteamUser();
        user.setSteamId(steamId);
        user.setUsername("joueur " + steamId);
        steamUserRepository.save(user);
        deckRepository.save(new Deck(user));
        return user;
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
