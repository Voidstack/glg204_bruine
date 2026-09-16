package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.repository.UserCardRepository;
import com.enosistudio.bruine.common.InsufficientScoreException;
import com.enosistudio.bruine.gacha.dto.GachaConfigFormDTO;
import com.enosistudio.bruine.gacha.dto.GachaResultDTO;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaConfigRepository;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({GachaService.class, SteamUserService.class, GachaRewardService.class})
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
    private SteamUserRepository steamUserRepository;

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

    @Test
    void aRarityWithoutAnyCardStillCostsThePull() {
        gachaRewardRepository.deleteAll();

        GachaResultDTO result = gachaService.spin(player, 1);

        assertEquals(90, player.getScore(), "le tirage est débité malgré tout");
        assertEquals(1, result.results().size());
        assertEquals(0, userCardRepository.count());
    }

    @Test
    void aMissingConfigurationFallsBackToTheDefaults() {
        gachaConfigRepository.deleteAll();

        GachaConfig fallback = gachaService.currentConfig();

        assertEquals(1, fallback.getXpMultNormal());
        assertEquals(4, fallback.getXpMultPolychrome());
    }

    @Test
    void aMultiplierBelowOneIsSavedAsOne() {
        GachaConfig saved = gachaService.saveConfig(new GachaConfigFormDTO(
                0, 100, 0, 0, 0,
                0, 0, 0, 0, 0,
                10, 25, 75, 200, 500,
                0, -2, 3, 5, 4));

        assertEquals(1, saved.xpMultiplierFor(ECardFinish.NORMAL));
        assertEquals(1, saved.xpMultiplierFor(ECardFinish.HOLOGRAPHIC));
        assertEquals(3, saved.xpMultiplierFor(ECardFinish.FOIL));
    }

    private SteamUser createPlayer(int score) {
        SteamUser user = new SteamUser();
        user.setSteamId("76561190000000001");
        user.setUsername("joueur");
        user.setScore(score);
        return steamUserRepository.save(user);
    }

    private void createReward(ECardRarity rarity) {
        GachaReward reward = new GachaReward();
        reward.setRarity(rarity);
        reward.setName("Carte " + rarity);
        reward.setEmoji("*");
        gachaRewardRepository.save(reward);
    }

    private void onlyEpicCanBeDrawn() {
        GachaConfig config = new GachaConfig();
        config.setRarityEpic(100);
        gachaConfigRepository.save(config);
    }
}
