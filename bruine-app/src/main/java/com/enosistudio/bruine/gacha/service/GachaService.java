package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.dto.CardViewDTO;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.repository.UserCardRepository;
import com.enosistudio.bruine.common.InsufficientScoreException;
import com.enosistudio.bruine.gacha.dto.GachaConfigFormDTO;
import com.enosistudio.bruine.gacha.dto.GachaResultDTO;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaConfigRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Règles du tirage gacha : coût, tirage de la rareté et de la finition, débit du score
 * et attribution des cartes.
 * <p>
 * Le tirage est transactionnel : il débite le joueur puis lui crée ses cartes, et ces
 * deux effets doivent réussir ou échouer ensemble, sinon un incident laisserait le
 * joueur débité sans contrepartie.
 */
@Service
public class GachaService {

    /**
     * Coût en points d'un tirage unitaire.
     */
    public static final int COST_PER_PULL = 10;

    /**
     * Nombre maximal de cartes tirées en un seul appel.
     */
    public static final int MAX_PULLS_PER_SPIN = 10;

    /**
     * La configuration du gacha est une ligne unique, créée par la migration Flyway.
     */
    private static final int CONFIG_ID = 1;

    private final SteamUserService steamUserService;
    private final GachaRewardService gachaRewardService;
    private final GachaConfigRepository gachaConfigRepository;
    private final UserCardRepository userCardRepository;

    public GachaService(SteamUserService steamUserService,
                        GachaRewardService gachaRewardService,
                        GachaConfigRepository gachaConfigRepository,
                        UserCardRepository userCardRepository) {
        this.steamUserService = steamUserService;
        this.gachaRewardService = gachaRewardService;
        this.gachaConfigRepository = gachaConfigRepository;
        this.userCardRepository = userCardRepository;
    }

    /**
     * Configuration courante, ou une configuration par défaut si la ligne est absente.
     */
    @Transactional(readOnly = true)
    public GachaConfig currentConfig() {
        return gachaConfigRepository.findById(CONFIG_ID).orElseGet(GachaConfig::new);
    }

    /**
     * Enregistre la configuration saisie par l'administrateur. Les poids et barèmes
     * négatifs sont ramenés à 0, les multiplicateurs à 1 au minimum.
     */
    @Transactional
    public GachaConfig saveConfig(GachaConfigFormDTO form) {
        GachaConfig config = currentConfig();
        config.setRarityLegendary(Math.max(form.rarityLegendary(), 0));
        config.setRarityEpic(Math.max(form.rarityEpic(), 0));
        config.setRarityRare(Math.max(form.rarityRare(), 0));
        config.setRarityUncommon(Math.max(form.rarityUncommon(), 0));
        config.setRarityCommon(Math.max(form.rarityCommon(), 0));
        config.setFinishNegative(Math.max(form.finishNegative(), 0));
        config.setFinishPolychrome(Math.max(form.finishPolychrome(), 0));
        config.setFinishFoil(Math.max(form.finishFoil(), 0));
        config.setFinishHolographic(Math.max(form.finishHolographic(), 0));
        config.setFinishNormal(Math.max(form.finishNormal(), 0));
        config.setXpBaseCommon(Math.max(form.xpBaseCommon(), 0));
        config.setXpBaseUncommon(Math.max(form.xpBaseUncommon(), 0));
        config.setXpBaseRare(Math.max(form.xpBaseRare(), 0));
        config.setXpBaseEpic(Math.max(form.xpBaseEpic(), 0));
        config.setXpBaseLegendary(Math.max(form.xpBaseLegendary(), 0));
        config.setXpMultNormal(Math.max(form.xpMultNormal(), 1));
        config.setXpMultHolographic(Math.max(form.xpMultHolographic(), 1));
        config.setXpMultFoil(Math.max(form.xpMultFoil(), 1));
        config.setXpMultNegative(Math.max(form.xpMultNegative(), 1));
        config.setXpMultPolychrome(Math.max(form.xpMultPolychrome(), 1));
        return gachaConfigRepository.save(config);
    }

    /**
     * Effectue un tirage pour le joueur et lui attribue les cartes obtenues.
     *
     * @param requestedPulls nombre de tirages demandé, ramené dans les bornes autorisées
     * @throws InsufficientScoreException si le score ne couvre pas le coût total
     */
    @Transactional
    public GachaResultDTO spin(SteamUser user, int requestedPulls) {
        int pulls = Math.clamp(requestedPulls, 1, MAX_PULLS_PER_SPIN);
        int totalCost = pulls * COST_PER_PULL;

        SteamUser steanUser = steamUserService.lock(user.getId());
        if (steanUser.getScore() < totalCost) {
            throw new InsufficientScoreException(totalCost);
        }

        steanUser.setScore(steanUser.getScore() - totalCost);
        steanUser.setTotalPulls(steanUser.getTotalPulls() + pulls);

        GachaConfig config = currentConfig();
        List<CardViewDTO> drawn = new ArrayList<>(pulls);
        for (int i = 0; i < pulls; i++) {
            drawn.add(drawOne(steanUser, config));
        }
        return new GachaResultDTO(drawn, steanUser.getScore(), totalCost);
    }

    private CardViewDTO drawOne(SteamUser user, GachaConfig config) {
        ECardRarity rarity = ECardRarity.getRandomRarityFromConfig(config);
        ECardFinish finish = ECardFinish.getRandomFinishFromConfig(config);
        GachaReward reward = pickReward(rarity);

        if (reward != null) {
            UserCard card = new UserCard();
            card.setSteamUser(user);
            card.setGachaReward(reward);
            card.setFinish(finish);
            userCardRepository.save(card);
        }
        return CardViewDTO.drawn(rarity, reward, finish);
    }

    /**
     * Carte au hasard parmi celles de la rareté tirée, ou null si l'administrateur n'en a défini aucune.
     */
    private GachaReward pickReward(ECardRarity rarity) {
        List<GachaReward> pool = gachaRewardService.findByRarity(rarity);
        return pool.isEmpty() ? null : pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

}
