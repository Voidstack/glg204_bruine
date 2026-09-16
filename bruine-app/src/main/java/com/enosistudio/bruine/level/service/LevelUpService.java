package com.enosistudio.bruine.level.service;

import com.enosistudio.bruine.card.*;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.level.dto.XpConvertibleCardDTO;
import com.enosistudio.bruine.level.dto.XpLevelConvertRequestDTO;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                .map(card -> new XpConvertibleCardDTO(
                        card.reward(),
                        card.finish(),
                        card.count(),
                        computeXp(card.reward().getRarity(), card.finish(), config)))
                .toList();
    }

    /**
     * Détruit un exemplaire libre par demande recevable et crédite l'expérience correspondante.
     * <p>
     * Une demande est ignorée si le joueur n'a plus d'exemplaire libre (ni en vente, ni dans le deck) de cette carte.
     */
    @Transactional
    public long convert(SteamUser user, List<XpLevelConvertRequestDTO> requests) {
        // verrou pris en premier : les lectures suivantes voient l'état laissé par une conversion concurrente
        SteamUser player = steamUserService.lock(user.getId());
        GachaConfig config = gachaService.currentConfig();
        List<UserCard> freeCards = new ArrayList<>(userCardService.findFree(player.getId()));
        long xpGained = 0;

        for (XpLevelConvertRequestDTO request : requests) {
            Optional<UserCard> convertible = takeFreeCard(freeCards, request.rewardId(), request.finish());
            if (convertible.isEmpty()) {
                continue;
            }
            UserCard card = convertible.get();
            xpGained += computeXp(card.getGachaReward().getRarity(), card.getFinish(), config);
            userCardRepository.delete(card);
        }

        player.setTotalExperience(player.getTotalExperience() + xpGained);
        return xpGained;
    }

    /**
     * Retire des cartes libres le premier exemplaire correspondant, pour qu'une demande suivante n'y touche plus.
     */
    private Optional<UserCard> takeFreeCard(List<UserCard> freeCards, Long rewardId, ECardFinish finish) {
        Optional<UserCard> card = freeCards.stream()
                .filter(candidate -> candidate.getGachaReward().getId().equals(rewardId)
                        && candidate.getFinish() == finish)
                .findFirst();
        card.ifPresent(freeCards::remove);
        return card;
    }

    private int computeXp(ECardRarity rarity, ECardFinish finish, GachaConfig config) {
        return config.xpBaseFor(rarity) * config.xpMultiplierFor(finish);
    }
}
