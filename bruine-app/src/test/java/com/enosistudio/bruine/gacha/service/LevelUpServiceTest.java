package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaConfigRepository;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.level.dto.ConvertRequestDTO;
import com.enosistudio.bruine.level.dto.ConvertResultDTO;
import com.enosistudio.bruine.level.dto.ConvertibleCardDTO;
import com.enosistudio.bruine.level.service.LevelUpService;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.market.service.MarketService;
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

/**
 * Règles de conversion des cartes en expérience.
 * <p>
 * La conversion détruit définitivement des cartes : ce qu'elle refuse compte donc autant
 * que ce qu'elle accorde.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({LevelUpService.class, SteamUserService.class, com.enosistudio.bruine.card.UserCardService.class,
        MarketService.class, DeckService.class, GachaService.class, GachaRewardService.class})
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

    private SteamUser owner;

    @BeforeEach
    void setUp() {
        owner = createOwner();
    }

    @Test
    void experienceIsTheRarityBaseWhenTheFinishIsPlain() {
        createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(500, convert(ECardRarity.LEGENDARY, ECardFinish.NORMAL).xpGained());
    }

    @Test
    void experienceScalesWithTheFinishMultiplier() {
        createCard(ECardRarity.LEGENDARY, ECardFinish.POLYCHROME);

        assertEquals(2000, convert(ECardRarity.LEGENDARY, ECardFinish.POLYCHROME).xpGained(),
                "polychrome vaut quatre fois la base dans la configuration par défaut");
    }

    @Test
    void eachRarityHasItsOwnBase() {
        createCard(ECardRarity.COMMON, ECardFinish.NORMAL);

        assertEquals(10, convert(ECardRarity.COMMON, ECardFinish.NORMAL).xpGained());
    }


    /**
     * Un administrateur peut mettre un multiplicateur à zéro dans la configuration.
     * Le barème le ramène à 1 : une carte détruite rapporte toujours quelque chose.
     */
    @Test
    void aMultiplierOfZeroStillPaysTheRarityBase() {
        GachaConfig config = new GachaConfig();
        config.setXpMultNormal(0);
        gachaConfigRepository.save(config);
        createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(500, convert(ECardRarity.LEGENDARY, ECardFinish.NORMAL).xpGained());
    }

    @Test
    void aCardListedOnTheMarketIsNotDestroyed() {
        UserCard listed = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        putOnSale(listed);

        ConvertResultDTO result = convert(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        assertEquals(0, result.xpGained());
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void aCardThePlayerDoesNotOwnIsIgnored() {
        GachaReward neverOwned = createReward(ECardRarity.LEGENDARY);

        ConvertResultDTO result = levelUpService.convert(reloadOwner(),
                List.of(new ConvertRequestDTO(neverOwned.getId(), "NORMAL")));

        assertEquals(0, result.xpGained());
    }

    /**
     * Le nom de finition vient du navigateur : une valeur inventée est écartée, pas fatale.
     */
    @Test
    void anUnknownFinishNameIsIgnored() {
        UserCard card = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);

        ConvertResultDTO result = levelUpService.convert(reloadOwner(),
                List.of(new ConvertRequestDTO(card.getGachaReward().getId(), "PLASMA")));

        assertEquals(0, result.xpGained());
        assertEquals(1, userCardRepository.count());
    }

    @Test
    void severalRequestsAreAddedUpAndCreditedToThePlayer() {
        owner.setTotalExperience(1000);
        UserCard legendary = createCard(ECardRarity.LEGENDARY, ECardFinish.NORMAL);
        UserCard foil = createCard(ECardRarity.COMMON, ECardFinish.FOIL);

        ConvertResultDTO result = levelUpService.convert(reloadOwner(), List.of(
                new ConvertRequestDTO(legendary.getGachaReward().getId(), "NORMAL"),
                new ConvertRequestDTO(foil.getGachaReward().getId(), "FOIL")));

        assertEquals(530, result.xpGained(), "500 pour la légendaire, 10 fois 3 pour le foil commun");
        assertEquals(1530, result.newTotalExperience());
        assertEquals(0, userCardRepository.count(), "les deux exemplaires sont détruits");
    }

    /**
     * Une conversion vide reste une opération valide : elle ne crédite rien.
     */
    @Test
    void convertingNothingChangesNothing() {
        owner.setTotalExperience(1000);

        ConvertResultDTO result = levelUpService.convert(reloadOwner(), List.of());

        assertEquals(0, result.xpGained());
        assertEquals(1000, result.newTotalExperience());
    }

    /**
     * La page annonce l'expérience avant destruction : elle doit annoncer le vrai barème.
     */
    @Test
    void theOfferedCardsCarryTheExperienceTheyWillPay() {
        createCard(ECardRarity.EPIC, ECardFinish.NORMAL);
        createCard(ECardRarity.EPIC, ECardFinish.NEGATIVE);

        List<ConvertibleCardDTO> offered = levelUpService.findConvertibleCards(reloadOwnerWithCards());

        assertEquals(2, offered.size());
        assertEquals(200, offered.get(0).xp(), "épique en finition simple");
        assertEquals(1000, offered.get(1).xp(), "épique multiplié par cinq pour le négatif");
    }

    private SteamUser createOwner() {
        SteamUser user = new SteamUser();
        user.setSteamId("76561190000000001");
        user.setUsername("joueur");
        return steamUserRepository.save(user);
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

    private ConvertResultDTO convert(ECardRarity rarity, ECardFinish finish) {
        Long rewardId = gachaRewardRepository.findByRarity(rarity).get(0).getId();
        return levelUpService.convert(reloadOwner(), List.of(new ConvertRequestDTO(rewardId, finish.name())));
    }

    /**
     * La conversion reçoit le joueur sans sa collection : elle supprime des cartes, et
     * Hibernate tenterait sinon de fusionner une collection contenant des lignes détruites.
     */
    private SteamUser reloadOwner() {
        entityManager.flush();
        entityManager.clear();
        return steamUserRepository.findById(owner.getId()).orElseThrow();
    }

    /**
     * L'affichage, lui, a besoin de la collection chargée.
     */
    private SteamUser reloadOwnerWithCards() {
        SteamUser reloaded = reloadOwner();
        reloaded.getCards().size();
        return reloaded;
    }
}
