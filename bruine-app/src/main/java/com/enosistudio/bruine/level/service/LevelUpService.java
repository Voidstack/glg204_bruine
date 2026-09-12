package com.enosistudio.bruine.level.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.UserCardService;
import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.level.dto.ConvertRequestDTO;
import com.enosistudio.bruine.level.dto.ConvertResultDTO;
import com.enosistudio.bruine.level.dto.ConvertibleCardDTO;
import com.enosistudio.bruine.market.service.MarketService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final MarketService marketService;
    private final UserCardService userCardService;
    private final GachaService gachaService;

    public LevelUpService(SteamUserService steamUserService,
                          UserCardRepository userCardRepository,
                          MarketService marketService,
                          UserCardService userCardService,
                          GachaService gachaService) {
        this.steamUserService = steamUserService;
        this.userCardRepository = userCardRepository;
        this.marketService = marketService;
        this.userCardService = userCardService;
        this.gachaService = gachaService;
    }

    /**
     * Cartes convertibles du joueur, avec l'expérience que rapporte chaque exemplaire.
     *
     * @param user joueur chargé avec sa collection
     */
    @Transactional(readOnly = true)
    public List<ConvertibleCardDTO> findConvertibleCards(SteamUser user) {
        GachaConfig config = gachaService.currentConfig();
        return userCardService.findOwnedCards(user).stream()
                .map(card -> new ConvertibleCardDTO(
                        card.reward(),
                        card.finish(),
                        card.count(),
                        computeXp(card.reward().getRarity(), card.finish(), config)))
                .toList();
    }

    /**
     * Détruit un exemplaire par demande recevable et crédite l'expérience correspondante.
     * <p>
     * Une demande est ignorée si sa finition est inconnue, si le joueur ne possède plus
     * la carte, ou si l'exemplaire est mis en vente sur le marché.
     *
     * @param user joueur chargé SANS sa collection : Hibernate tenterait sinon de
     *             fusionner une collection contenant des cartes déjà supprimées
     */
    @Transactional
    public ConvertResultDTO convert(SteamUser user, List<ConvertRequestDTO> requests) {
        GachaConfig config = gachaService.currentConfig();
        Set<Long> listedCardIds = marketService.findAllListedCardIds();
        long xpGained = 0;

        for (ConvertRequestDTO request : requests) {
            Optional<UserCard> convertible = findConvertible(user, request, listedCardIds);
            if (convertible.isEmpty()) {
                continue;
            }
            UserCard card = convertible.get();
            xpGained += computeXp(card.getGachaReward().getRarity(), card.getFinish(), config);
            userCardRepository.delete(card);
        }

        user.setTotalExperience(user.getTotalExperience() + xpGained);
        steamUserService.save(user);
        return new ConvertResultDTO(xpGained, user.getTotalExperience());
    }

    private Optional<UserCard> findConvertible(SteamUser user, ConvertRequestDTO request, Set<Long> listedCardIds) {
        return parseFinish(request.finish())
                .flatMap(finish -> userCardRepository
                        .findFirstBySteamUserIdAndGachaRewardIdAndFinish(user.getId(), request.rewardId(), finish))
                .filter(card -> !listedCardIds.contains(card.getId()));
    }

    private Optional<ECardFinish> parseFinish(String finish) {
        if (finish == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ECardFinish.valueOf(finish.toUpperCase()));
        } catch (IllegalArgumentException unknownFinish) {
            return Optional.empty();
        }
    }

    private int computeXp(ECardRarity rarity, ECardFinish finish, GachaConfig config) {
        int base = config.xpBaseFor(rarity);
        int multiplier = switch (finish) {
            case HOLOGRAPHIC -> config.getXpMultHolographic();
            case FOIL -> config.getXpMultFoil();
            case NEGATIVE -> config.getXpMultNegative();
            case POLYCHROME -> config.getXpMultPolychrome();
            default -> config.getXpMultNormal();
        };
        return base * Math.max(multiplier, 1);
    }
}
