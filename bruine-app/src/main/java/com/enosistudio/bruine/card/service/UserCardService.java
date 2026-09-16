package com.enosistudio.bruine.card.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.dto.CardStackDTO;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.repository.UserCardRepository;
import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public List<UserCard> findNotListed(Long userId) {
        Set<Long> listedIds = listedCardIds(userId);
        return userCardRepository.findBySteamUserIdOrderById(userId).stream()
                .filter(card -> !listedIds.contains(card.getId()))
                .toList();
    }

    /**
     * Cartes libres du joueur, ni en vente ni dans son deck : les seules qui peuvent être vendues ou converties.
     */
    private List<UserCard> findFree(Long userId) {
        Set<Long> deckIds = deckCardIds(userId);
        return findNotListed(userId).stream()
                .filter(card -> !deckIds.contains(card.getId()))
                .toList();
    }

    /**
     * Carte libre désignée par son identifiant, ou refus expliqué au joueur : seul moyen de viser
     * une carte à vendre ou à convertir, pour que la règle ne soit pas réécrite ailleurs.
     */
    @Transactional(readOnly = true)
    public UserCard requireFree(Long userId, Long cardId) {
        UserCard card = userCardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessRuleException("Carte introuvable."));
        if (!card.getSteamUser().getId().equals(userId)) {
            throw new BusinessRuleException("Cette carte ne vous appartient pas.");
        }
        if (marketListingRepository.existsByUserCardId(cardId)) {
            throw new BusinessRuleException("Cette carte est déjà en vente.");
        }
        if (deckRepository.existsByCardsId(cardId)) {
            throw new BusinessRuleException("Cette carte est dans votre deck, retirez-la du deck d'abord.");
        }
        return card;
    }

    @Transactional(readOnly = true)
    public List<UserCard> requireAllFree(Long userId, List<Long> cardIds) {
        Map<Long, UserCard> freeById = findFree(userId).stream()
                .collect(Collectors.toMap(UserCard::getId, card -> card));
        return cardIds.stream()
                .distinct()
                .map(cardId -> freeById.containsKey(cardId) ? freeById.get(cardId) : requireFree(userId, cardId))
                .toList();
    }

    /**
     * Cartes non vendues du joueur, empilées et triées par rareté décroissante.
     */
    @Transactional(readOnly = true)
    public List<CardStackDTO> findOwnedCards(Long userId) {
        return groupByCardSortedByRarity(findNotListed(userId));
    }

    /**
     * Cartes libres du joueur, empilées et triées par rareté décroissante.
     */
    @Transactional(readOnly = true)
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

    private Set<Long> listedCardIds(Long userId) {
        return marketListingRepository.findByUserCardSteamUserId(userId).stream()
                .map(listing -> listing.getUserCard().getId())
                .collect(Collectors.toSet());
    }

    private Set<Long> deckCardIds(Long userId) {
        return deckRepository.findWithCardsBySteamUserId(userId)
                .map(Deck::getCards)
                .orElse(List.of())
                .stream()
                .map(UserCard::getId)
                .collect(Collectors.toSet());
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
