package com.enosistudio.bruine.level.service;

import com.enosistudio.bruine.card.*;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaConfigRepository;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.gacha.service.GachaRewardService;
import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.level.dto.ConvertRequestDTO;
import com.enosistudio.bruine.level.dto.ConvertibleCardDTO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@Import({LevelUpService.class, SteamUserService.class, UserCardService.class,
        DeckService.class, GachaService.class, GachaRewardService.class})
class LevelUpServiceTest {

    @Autowired
    private LevelUpService levelUpService;

    @Autowired
    private GachaConfigRepository gachaConfigRepository;

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
        owner = createOwner();
    }

    @Test
    void experienceIsTheRarityBaseWhenTheFinishIsPlain() {
        createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(500, convert(ECardRarity.LEGENDARY, ECardFinish.NORMAL));
    }

    @Test
    void experienceScalesWithTheFinishMultiplier() {
        createCard(ECardRarity.LEGENDARY, ECardFinish.POLYCHROME);

        assertEquals(2000, convert(ECardRarity.LEGENDARY, ECardFinish.POLYCHROME),
                "polychrome vaut quatre fois la base dans la configuration par défaut");
    }

    @Test
    void eachRarityHasItsOwnBase() {
        createCard(ECardRarity.COMMON, ECardFinish.NORMAL);

        assertEquals(10, convert(ECardRarity.COMMON, ECardFinish.NORMAL));
    }

    @Test
    void aMultiplierOfZeroStillPaysTheRarityBase() {
        GachaConfig config = new GachaConfig();
        config.setXpMultNormal(0);
        gachaConfigRepository.save(config);
        createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(500, convert(ECardRarity.LEGENDARY, ECardFinish.NORMAL));
    }

    @Test
    void aCardListedOnTheMarketIsNotDestroyed() {
        UserCard listed = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        putOnSale(listed);

        long xpGained = convert(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(0, xpGained);
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void aCardPlacedInTheDeckIsNotDestroyed() {
        UserCard inDeck = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        deckService.saveDeck(owner.getId(), List.of(inDeck.getId()));

        long xpGained = convert(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(0, xpGained);
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void theFreeCopyIsConvertedWhileTheDeckCopyStays() {
        UserCard inDeck = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        UserCard spare = new UserCard();
        spare.setSteamUser(owner);
        spare.setGachaReward(inDeck.getGachaReward());
        spare.setFinish(ECardFinish.NORMAL);
        userCardRepository.save(spare);
        deckService.saveDeck(owner.getId(), List.of(inDeck.getId()));

        long xpGained = levelUpService.convert(reloadOwner(),
                List.of(new ConvertRequestDTO(inDeck.getGachaReward().getId(), "NORMAL")));

        assertEquals(500, xpGained);
        assertEquals(List.of(inDeck.getId()), userCardRepository.findAll().stream().map(UserCard::getId).toList());
    }

    @Test
    void aCardThePlayerDoesNotOwnIsIgnored() {
        GachaReward neverOwned = createReward(ECardRarity.LEGENDARY);

        long xpGained = levelUpService.convert(reloadOwner(),
                List.of(new ConvertRequestDTO(neverOwned.getId(), "NORMAL")));

        assertEquals(0, xpGained);
    }

    @Test
    void anUnknownFinishNameIsIgnored() {
        UserCard card = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        long xpGained = levelUpService.convert(reloadOwner(),
                List.of(new ConvertRequestDTO(card.getGachaReward().getId(), "PLASMA")));

        assertEquals(0, xpGained);
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void severalRequestsAreAddedUpAndCreditedToThePlayer() {
        owner.setTotalExperience(1000);
        UserCard legendary = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        UserCard foil = createCard(ECardRarity.COMMON, ECardFinish.FOIL);

        long xpGained = levelUpService.convert(reloadOwner(), List.of(
                new ConvertRequestDTO(legendary.getGachaReward().getId(), "NORMAL"),
                new ConvertRequestDTO(foil.getGachaReward().getId(), "FOIL")));

        assertEquals(530, xpGained, "500 pour la légendaire, 10 fois 3 pour le foil commun");
        assertEquals(1530, reloadOwner().getTotalExperience());
        assertEquals(0, userCardRepository.count(), "les deux exemplaires sont détruits");
    }

    @Test
    void convertingNothingChangesNothing() {
        owner.setTotalExperience(1000);

        long xpGained = levelUpService.convert(reloadOwner(), List.of());

        assertEquals(0, xpGained);
        assertEquals(1000, reloadOwner().getTotalExperience());
    }

    @Test
    void theOfferedCardsCarryTheExperienceTheyWillPay() {
        createCard(ECardRarity.EPIC, ECardFinish.NORMAL);
        createCard(ECardRarity.EPIC, ECardFinish.NEGATIVE);

        List<ConvertibleCardDTO> offered = levelUpService.findConvertibleCards(reloadOwner().getId());

        assertEquals(2, offered.size());
        assertEquals(200, offered.get(0).xp(), "épique en finition simple");
        assertEquals(1000, offered.get(1).xp(), "épique multiplié par cinq pour le négatif");
    }

    private SteamUser createOwner() {
        SteamUser user = new SteamUser();
        user.setSteamId("76561190000000001");
        user.setUsername("joueur");
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

    private UserCard createCard(ECardRarity rarity, ECardFinish finish) {
        UserCard card = new UserCard();
        card.setSteamUser(owner);
        card.setGachaReward(createReward(rarity));
        card.setFinish(finish);
        return userCardRepository.save(card);
    }

    private void putOnSale(UserCard card) {
        MarketListing listing = new MarketListing();
        listing.setSeller(card.getSteamUser());
        listing.setUserCard(card);
        listing.setPrice(10);
        marketListingRepository.save(listing);
    }

    private long convert(ECardRarity rarity, ECardFinish finish) {
        Long rewardId = gachaRewardRepository.findByRarity(rarity).get(0).getId();
        return levelUpService.convert(reloadOwner(), List.of(new ConvertRequestDTO(rewardId, finish.name())));
    }

    private SteamUser reloadOwner() {
        entityManager.flush();
        entityManager.clear();
        return steamUserRepository.findById(owner.getId()).orElseThrow();
    }
}
