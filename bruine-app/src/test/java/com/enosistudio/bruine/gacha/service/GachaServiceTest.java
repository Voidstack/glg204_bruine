package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.gacha.dto.GachaResultDTO;
import com.enosistudio.bruine.gacha.exception.InsufficientScoreException;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaConfigRepository;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.UserRepository;
import com.enosistudio.bruine.steam.security.SteamUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Règles du tirage gacha : ce qu'il coûte, ce qu'il donne, et ce qu'il refuse.
 * <p>
 * Le tirage étant aléatoire, les tests ne devinent pas son résultat : ils vérifient ce
 * qui est vrai à tous les coups. Une rareté de poids nul ne peut jamais sortir, et une
 * configuration entièrement à zéro ne doit pas faire échouer le tirage.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({GachaService.class, SteamUserService.class, com.enosistudio.bruine.gacha.service.GachaRewardService.class})
class GachaServiceTest {

    @Autowired
    private GachaService gachaService;

    @Autowired
    private GachaConfigRepository gachaConfigRepository;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private UserRepository userRepository;

    private SteamUser player;

    @BeforeEach
    void setUp() {
        player = createPlayer(100);
        createReward(ECardRarity.EPIC);
        onlyEpicCanBeDrawn();
    }

    @Test
    void eachPullCostsTenPoints() {
        GachaResultDTO result = gachaService.spin(player, 3);

        assertEquals(30, result.totalCost());
        assertEquals(70, player.getScore());
        assertEquals(70, result.score(), "le résultat renvoie le score après débit");
    }

    @Test
    void pullsAreCountedInThePlayerTotal() {
        player.setTotalPulls(12);

        gachaService.spin(player, 3);

        assertEquals(15, player.getTotalPulls());
    }

    /**
     * Le refus doit précéder le débit : un joueur trop pauvre ne perd rien.
     */
    @Test
    void spinningIsRefusedWhenTheScoreDoesNotCoverTheCost() {
        player.setScore(25);

        assertThrows(InsufficientScoreException.class, () -> gachaService.spin(player, 3));

        assertEquals(25, player.getScore());
        assertEquals(0, userCardRepository.count());
    }

    @Test
    void spinningIsAllowedWhenTheScoreCoversTheCostExactly() {
        player.setScore(30);

        assertEquals(3, gachaService.spin(player, 3).results().size());
        assertEquals(0, player.getScore());
    }

    @Test
    void aPullCountBelowOneIsBroughtBackToOne() {
        GachaResultDTO result = gachaService.spin(player, 0);

        assertEquals(1, result.results().size());
        assertEquals(10, result.totalCost());
    }

    @Test
    void aPullCountAboveTenIsCappedAtTen() {
        GachaResultDTO result = gachaService.spin(player, 99);

        assertEquals(10, result.results().size());
        assertEquals(100, result.totalCost(), "on ne paie que les dix tirages accordés");
    }

    @Test
    void everyPullAddsOneCardToTheCollection() {
        gachaService.spin(player, 3);

        assertEquals(3, userCardRepository.count());
    }

    @Test
    void onlyARarityWithAWeightCanBeDrawn() {
        GachaResultDTO result = gachaService.spin(player, 10);

        assertTrue(result.results().stream().allMatch(card -> ECardRarity.EPIC == card.rarity()),
                "une rareté de poids nul ne doit jamais sortir : " + result.results());
    }

    /**
     * Les poids de finition sont tous à zéro dans cette configuration : le tirage doit
     * retomber sur la finition simple au lieu de diviser par zéro.
     */
    @Test
    void finishesWithoutAnyWeightFallBackToThePlainOne() {
        GachaResultDTO result = gachaService.spin(player, 10);

        assertTrue(result.results().stream().allMatch(card -> card.finish() == ECardFinish.NORMAL),
                "finitions obtenues : " + result.results());
    }

    @Test
    void aConfigurationEntirelyAtZeroStillDraws() {
        gachaConfigRepository.deleteAll();
        createReward(ECardRarity.COMMON);

        GachaResultDTO result = gachaService.spin(player, 1);

        assertEquals(ECardRarity.COMMON, result.results().get(0).rarity(),
                "sans aucun poids, le tirage retombe sur la rareté la plus basse");
    }

    /**
     * Une rareté dont l'administrateur n'a créé aucune carte : le joueur paie son tirage
     * et reçoit une carte de repli, mais rien n'entre dans sa collection.
     */
    @Test
    void aRarityWithoutAnyCardStillCostsThePull() {
        gachaRewardRepository.deleteAll();

        GachaResultDTO result = gachaService.spin(player, 1);

        assertEquals(90, player.getScore(), "le tirage est débité malgré tout");
        assertEquals(1, result.results().size());
        assertEquals(0, userCardRepository.count());
    }

    /**
     * La configuration est une ligne unique ; son absence ne doit pas bloquer le jeu.
     */
    @Test
    void aMissingConfigurationFallsBackToTheDefaults() {
        gachaConfigRepository.deleteAll();

        GachaConfig fallback = gachaService.currentConfig();

        assertEquals(1, fallback.getXpMultNormal());
        assertEquals(4, fallback.getXpMultPolychrome());
    }

    private SteamUser createPlayer(int score) {
        SteamUser user = new SteamUser();
        user.setSteamId("76561190000000001");
        user.setUsername("joueur");
        user.setScore(score);
        return userRepository.save(user);
    }

    private void createReward(ECardRarity rarity) {
        GachaReward reward = new GachaReward();
        reward.setRarity(rarity);
        reward.setName("Carte " + rarity);
        reward.setEmoji("*");
        gachaRewardRepository.save(reward);
    }

    /**
     * Ne laisse qu'une rareté possible, pour que le tirage reste prévisible.
     */
    private void onlyEpicCanBeDrawn() {
        GachaConfig config = new GachaConfig();
        config.setRarityEpic(100);
        gachaConfigRepository.save(config);
    }
}
