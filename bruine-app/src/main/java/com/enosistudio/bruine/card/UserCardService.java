package com.enosistudio.bruine.card;

import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.market.service.MarketService;
import com.enosistudio.bruine.steam.model.SteamUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Vue de la collection d'un joueur, regroupée et triée pour l'affichage.
 */
@Service
public class UserCardService {

    private final MarketService marketService;

    public UserCardService(MarketService marketService) {
        this.marketService = marketService;
    }

    /**
     * Cartes que le joueur détient réellement, triées par rareté décroissante.
     * Les cartes mises en vente sont exclues : elles sont bloquées tant que l'annonce court.
     *
     * @param user joueur chargé avec sa collection
     */
    @Transactional(readOnly = true)
    public List<CardStackDTO> findOwnedCards(SteamUser user) {
        Set<Long> listedCardIds = marketService.findAllListedCardIds();
        List<UserCard> owned = user.getCards().stream()
                .filter(card -> !listedCardIds.contains(card.getId()))
                .toList();
        return groupByCardSortedByRarity(owned);
    }

    /**
     * Nombre d'exemplaires détenus, toutes finitions confondues.
     */
    public long countOwned(List<CardStackDTO> cards) {
        return cards.stream().mapToLong(CardStackDTO::count).sum();
    }

    /**
     * Nombre de récompenses distinctes détenues, sans tenir compte de la finition.
     */
    public long countDistinctRewards(List<CardStackDTO> cards) {
        return cards.stream().map(card -> card.reward().getId()).distinct().count();
    }

    /**
     * Regroupe les exemplaires par récompense et finition, puis produit une liste plate
     * ordonnée par rareté. L'ordre d'insertion est conservé à l'intérieur d'une rareté,
     * comme l'ordre des exemplaires à l'intérieur d'une pile.
     */
    private List<CardStackDTO> groupByCardSortedByRarity(List<UserCard> owned) {
        record CardKey(Long rewardId, ECardFinish finish) {
        }

        Map<CardKey, GachaReward> rewardByKey = new LinkedHashMap<>();
        Map<CardKey, List<Long>> idsByKey = new LinkedHashMap<>();
        for (UserCard card : owned) {
            CardKey key = new CardKey(card.getGachaReward().getId(), card.getFinish());
            rewardByKey.putIfAbsent(key, card.getGachaReward());
            idsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(card.getId());
        }

        // De la plus rare à la plus commune, soit l'ordre de déclaration de l'enum inversé
        List<ECardRarity> rarities = new ArrayList<>(List.of(ECardRarity.values()));
        Collections.reverse(rarities);

        List<CardStackDTO> sorted = new ArrayList<>(rewardByKey.size());
        for (ECardRarity rarity : rarities) {
            rewardByKey.forEach((key, reward) -> {
                if (rarity == reward.getRarity()) {
                    sorted.add(new CardStackDTO(reward, key.finish(), List.copyOf(idsByKey.get(key))));
                }
            });
        }
        return sorted;
    }
}
