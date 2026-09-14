package com.enosistudio.bruine.card;

import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Collection d'un joueur : seul endroit qui décide quelles cartes sont en vente, dans le deck ou libres.
 */
@Service
public class UserCardService {

    private final UserCardRepository userCardRepository;
    private final MarketListingRepository marketListingRepository;
    private final DeckRepository deckRepository;

    public UserCardService(UserCardRepository userCardRepository,
                           MarketListingRepository marketListingRepository,
                           DeckRepository deckRepository) {
        this.userCardRepository = userCardRepository;
        this.marketListingRepository = marketListingRepository;
        this.deckRepository = deckRepository;
    }

    /**
     * Cartes du joueur qui ne sont pas en vente : elles sont bloquées tant que l'annonce court.
     * Les cartes du deck en font partie.
     */
    public List<UserCard> findNotListed(Long userId) {
        Set<Long> listedIds = marketListingRepository.findBySellerIdOrderByCreatedAtDesc(userId).stream()
                .map(listing -> listing.getUserCard().getId())
                .collect(Collectors.toSet());
        return userCardRepository.findBySteamUserIdOrderById(userId).stream()
                .filter(card -> !listedIds.contains(card.getId()))
                .toList();
    }

    /**
     * Cartes libres du joueur, ni en vente ni dans son deck : les seules qui peuvent être vendues ou converties.
     */
    public List<UserCard> findFree(Long userId) {
        Set<Long> deckIds = deckRepository.findWithCardsBySteamUserId(userId)
                .map(Deck::getCards)
                .orElse(List.of())
                .stream()
                .map(UserCard::getId)
                .collect(Collectors.toSet());
        return findNotListed(userId).stream()
                .filter(card -> !deckIds.contains(card.getId()))
                .toList();
    }

    /**
     * Cartes non vendues du joueur, empilées et triées par rareté décroissante.
     */
    public List<CardStackDTO> findOwnedCards(Long userId) {
        return groupByCardSortedByRarity(findNotListed(userId));
    }

    /**
     * Cartes libres du joueur, empilées et triées par rareté décroissante.
     */
    public List<CardStackDTO> findFreeCards(Long userId) {
        return groupByCardSortedByRarity(findFree(userId));
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
        List<ECardRarity> rarities = new ArrayList<>(List.of(ECardRarity.values())).reversed();

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
