package com.enosistudio.bruine.level.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.repository.UserCardRepository;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.level.dto.XpConvertibleCardDTO;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Conversion de cartes en expérience.
 * L'ex d'une carte fixée par rareté, multipliée par un
 * coefficient de finition venant de la configuration du gacha.
 * La conversion est transactionnelle, détruit des cartes puis crédite le user
 */
@Service
public class LevelUpService {

    private final SteamUserService steamUserService;
    private final UserCardRepository userCardRepository;
    private final UserCardService userCardService;
    private final GachaService gachaService;

    public LevelUpService(SteamUserService steamUserService,
                          UserCardRepository userCardRepository,
                          UserCardService userCardService,
                          GachaService gachaService) {
        this.steamUserService = steamUserService;
        this.userCardRepository = userCardRepository;
        this.userCardService = userCardService;
        this.gachaService = gachaService;
    }

    /**
     * Cartes convertibles du joueur (ni en vente, ni dans le deck), avec l'expérience que rapporte chaque exemplaire.
     */
    @Transactional(readOnly = true)
    public List<XpConvertibleCardDTO> findConvertibleCards(Long userId) {
        GachaConfig config = gachaService.currentConfig();
        return userCardService.findFreeCards(userId).stream()
                .map(stack -> new XpConvertibleCardDTO(stack,
                        computeXp(stack.reward().getRarity(), stack.finish(), config)))
                .toList();
    }

    /**
     * Détruit les cartes désignées et crédite l'expérience correspondante. Une seule carte qui n'est plus libre
     * (vendue, mise en vente ou posée dans le deck depuis l'affichage) fait refuser toute la conversion.
     */
    @Transactional
    public long convert(SteamUser user, List<Long> cardIds) {
        // verrou pris en premier : les lectures suivantes voient l'état laissé par une conversion concurrente
        SteamUser player = steamUserService.lock(user.getId());
        GachaConfig config = gachaService.currentConfig();
        List<UserCard> cards = userCardService.requireAllFree(player.getId(), cardIds);

        long xpGained = cards.stream()
                .mapToLong(card -> computeXp(card.getGachaReward().getRarity(), card.getFinish(), config))
                .sum();
        userCardRepository.deleteAll(cards);

        player.setTotalExperience(player.getTotalExperience() + xpGained);
        return xpGained;
    }

    private int computeXp(ECardRarity rarity, ECardFinish finish, GachaConfig config) {
        return config.xpBaseFor(rarity) * config.xpMultiplierFor(finish);
    }
}
