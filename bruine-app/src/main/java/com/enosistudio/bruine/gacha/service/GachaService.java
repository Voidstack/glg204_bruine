package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.CardViewDTO;
import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.gacha.dto.GachaResultDTO;
import com.enosistudio.bruine.gacha.exception.InsufficientScoreException;
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
     * Effectue un tirage pour le joueur et lui attribue les cartes obtenues.
     *
     * @param requestedPulls nombre de tirages demandé, ramené dans les bornes autorisées
     * @throws InsufficientScoreException si le score ne couvre pas le coût total
     */
    @Transactional
    public GachaResultDTO spin(SteamUser user, int requestedPulls) {
        int pulls = Math.min(Math.max(requestedPulls, 1), MAX_PULLS_PER_SPIN);
        int totalCost = pulls * COST_PER_PULL;

        if (user.getScore() < totalCost) {
            throw new InsufficientScoreException(user.getScore(), totalCost);
        }

        user.setScore(user.getScore() - totalCost);
        user.setTotalPulls(user.getTotalPulls() + pulls);
        steamUserService.save(user);

        GachaConfig config = currentConfig();
        List<CardViewDTO> drawn = new ArrayList<>(pulls);
        for (int i = 0; i < pulls; i++) {
            drawn.add(drawOne(user, config));
        }
        return new GachaResultDTO(drawn, user.getScore(), totalCost);
    }

    private CardViewDTO drawOne(SteamUser user, GachaConfig config) {
        ECardRarity rarity = rollRarity(config);
        ECardFinish finish = rollFinish(config);
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

    /**
     * Tirage pondéré de la rareté. Les poids de la configuration ne sont pas des
     * pourcentages : on tire dans leur somme puis on retranche palier par palier.
     */
    private ECardRarity rollRarity(GachaConfig config) {
        int total = config.getRarityLegendary() + config.getRarityEpic() + config.getRarityRare()
                + config.getRarityUncommon() + config.getRarityCommon();
        int draw = randomBelow(total);

        if (draw < config.getRarityLegendary()) return ECardRarity.LEGENDARY;
        draw -= config.getRarityLegendary();
        if (draw < config.getRarityEpic()) return ECardRarity.EPIC;
        draw -= config.getRarityEpic();
        if (draw < config.getRarityRare()) return ECardRarity.RARE;
        draw -= config.getRarityRare();
        if (draw < config.getRarityUncommon()) return ECardRarity.UNCOMMON;
        return ECardRarity.COMMON;
    }

    /**
     * Tirage pondéré de la finition, sur le même principe que la rareté.
     */
    private ECardFinish rollFinish(GachaConfig config) {
        int total = config.getFinishNegative() + config.getFinishPolychrome() + config.getFinishFoil()
                + config.getFinishHolographic() + config.getFinishNormal();
        int draw = randomBelow(total);

        if (draw < config.getFinishNegative()) return ECardFinish.NEGATIVE;
        draw -= config.getFinishNegative();
        if (draw < config.getFinishPolychrome()) return ECardFinish.POLYCHROME;
        draw -= config.getFinishPolychrome();
        if (draw < config.getFinishFoil()) return ECardFinish.FOIL;
        draw -= config.getFinishFoil();
        if (draw < config.getFinishHolographic()) return ECardFinish.HOLOGRAPHIC;
        return ECardFinish.NORMAL;
    }

    /**
     * Protège du cas où tous les poids seraient à zéro, qui ferait échouer le tirage.
     */
    private int randomBelow(int total) {
        return ThreadLocalRandom.current().nextInt(Math.max(total, 1));
    }
}
