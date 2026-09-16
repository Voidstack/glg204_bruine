package com.enosistudio.bruine.level.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.repository.UserCardRepository;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.gacha.service.GachaRewardService;
import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.level.dto.XpConvertibleCardDTO;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@Import({LevelUpService.class, SteamUserService.class, UserCardService.class,
        DeckService.class, GachaService.class, GachaRewardService.class})
class LevelUpServiceTest {

    @Autowired
    private LevelUpService levelUpService;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private MarketListingRepository marketListingRepository;

    @Autowired
    private SteamUserRepository steamUserRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeckService deckService;

    @Autowired
    private DeckRepository deckRepository;

    private SteamUser owner;

    @BeforeEach
    void setUp() {
        owner = createUser("76561190000000001");
    }

    @Test
    void experienceIsTheRarityBaseWhenTheFinishIsPlain() {
        UserCard card = createCard(owner, ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(500, convert(card));
    }

    @Test
    void experienceScalesWithTheFinishMultiplier() {
        UserCard card = createCard(owner, ECardRarity.LEGENDARY, ECardFinish.POLYCHROME);

        assertEquals(2000, convert(card), "polychrome vaut quatre fois la base dans la configuration par défaut");
    }

    @Test
    void eachRarityHasItsOwnBase() {
        UserCard card = createCard(owner, ECardRarity.COMMON, ECardFinish.NORMAL);

        assertEquals(10, convert(card));
    }

    @Test
    void severalCardsAreAddedUpAndCreditedToThePlayer() {
        owner.setTotalExperience(1000);
        UserCard legendary = createCard(owner, ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        UserCard foil = createCard(owner, ECardRarity.COMMON, ECardFinish.FOIL);

        assertEquals(530, convert(legendary, foil), "500 pour la légendaire, 10 fois 3 pour le foil commun");
        assertEquals(1530, reloadOwner().getTotalExperience());
        assertEquals(0, userCardRepository.count(), "les deux exemplaires sont détruits");
    }

    @Test
    void theSameCardSentTwiceIsConvertedOnce() {
        UserCard card = createCard(owner, ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(500, convert(card, card));
        assertEquals(500, reloadOwner().getTotalExperience());
    }

    @Test
    void convertingNothingChangesNothing() {
        owner.setTotalExperience(1000);

        assertEquals(0, convert());
        assertEquals(1000, reloadOwner().getTotalExperience());
    }

    @Test
    void aCardListedOnTheMarketIsRefused() {
        UserCard listed = createCard(owner, ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        putOnSale(listed);

        assertThrows(BusinessRuleException.class, () -> convert(listed));
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void aCardPlacedInTheDeckIsRefused() {
        UserCard inDeck = createCard(owner, ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        deckService.saveDeck(owner.getId(), List.of(inDeck.getId()));

        assertThrows(BusinessRuleException.class, () -> convert(inDeck));
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void aCardOwnedBySomeoneElseIsRefused() {
        UserCard othersCard = createCard(createUser("76561190000000002"), ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertThrows(BusinessRuleException.class, () -> convert(othersCard));
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void oneCardThatIsNoLongerFreeRefusesTheWholeConversion() {
        owner.setTotalExperience(1000);
        UserCard free = createCard(owner, ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        UserCard listed = createCard(owner, ECardRarity.COMMON, ECardFinish.NORMAL);
        putOnSale(listed);

        assertThrows(BusinessRuleException.class, () -> convert(free, listed));
        assertEquals(2, userCardRepository.count());
        assertEquals(1000, reloadOwner().getTotalExperience());
    }

    @Test
    void theOfferedCardsCarryTheExperienceTheyWillPay() {
        createCard(owner, ECardRarity.EPIC, ECardFinish.NORMAL);
        createCard(owner, ECardRarity.EPIC, ECardFinish.NEGATIVE);

        List<XpConvertibleCardDTO> offered = levelUpService.findConvertibleCards(reloadOwner().getId());

        assertEquals(2, offered.size());
        assertEquals(200, offered.get(0).xp(), "épique en finition simple");
        assertEquals(1000, offered.get(1).xp(), "épique multiplié par cinq pour le négatif");
    }

    private SteamUser createUser(String steamId) {
        SteamUser user = new SteamUser();
        user.setSteamId(steamId);
        user.setUsername("joueur");
        steamUserRepository.save(user);
        deckRepository.save(new Deck(user));
        return user;
    }

    private UserCard createCard(SteamUser user, ECardRarity rarity, ECardFinish finish) {
        GachaReward reward = new GachaReward();
        reward.setRarity(rarity);
        reward.setName("Carte " + rarity);
        reward.setEmoji("*");
        gachaRewardRepository.save(reward);

        UserCard card = new UserCard();
        card.setSteamUser(user);
        card.setGachaReward(reward);
        card.setFinish(finish);
        return userCardRepository.save(card);
    }

    private void putOnSale(UserCard card) {
        MarketListing listing = new MarketListing();
        listing.setUserCard(card);
        listing.setPrice(10);
        marketListingRepository.save(listing);
    }

    private long convert(UserCard... cards) {
        return levelUpService.convert(reloadOwner(), Stream.of(cards).map(UserCard::getId).toList());
    }

    private SteamUser reloadOwner() {
        entityManager.flush();
        entityManager.clear();
        return steamUserRepository.findById(owner.getId()).orElseThrow();
    }
}
