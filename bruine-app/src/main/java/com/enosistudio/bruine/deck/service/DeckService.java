package com.enosistudio.bruine.deck.service;

import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserCardRepository userCardRepository;
    private final MarketListingRepository marketListingRepository;

    public DeckService(DeckRepository deckRepository,
                       UserCardRepository userCardRepository,
                       MarketListingRepository marketListingRepository) {
        this.deckRepository = deckRepository;
        this.userCardRepository = userCardRepository;
        this.marketListingRepository = marketListingRepository;
    }

    /**
     * Cartes du deck du joueur, dans l'ordre choisi.
     */
    @Transactional(readOnly = true)
    public List<UserCard> findDeckCards(Long userId) {
        return deckRepository.findWithCardsBySteamUserId(userId)
                .map(deck -> List.copyOf(deck.getCards()))
                .orElse(List.of());
    }

    /**
     * Identifiants des cartes du deck du joueur.
     */
    @Transactional(readOnly = true)
    public Set<Long> findDeckCardIds(Long userId) {
        return findDeckCards(userId).stream()
                .map(UserCard::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Remplace le contenu du deck par les cartes sélectionnées, dans l'ordre reçu.
     * Ne sont retenues que les 10 premières cartes existantes, appartenant au joueur et hors marché.
     */
    @Transactional
    public void saveDeck(Long userId, List<Long> cardIds) {
        Deck deck = deckRepository.findBySteamUserId(userId).orElseThrow();
        Set<Long> listedCardIds = marketListingRepository.findAllListedCardIds();

        List<UserCard> cards = cardIds == null ? List.of() : cardIds.stream()
                .distinct()
                .map(id -> userCardRepository.findById(id).orElse(null))
                .filter(card -> card != null
                        && card.getSteamUser().getId().equals(userId)
                        && !listedCardIds.contains(card.getId()))
                .limit(Deck.MAX_CARDS)
                .toList();

        // Nouvelle liste plutôt que clear() : Hibernate supprime les anciennes lignes de deck_card avant
        // d'insérer les nouvelles, sans conflit sur l'unicité d'une carte quand l'ordre change.
        deck.setCards(new ArrayList<>(cards));
    }
}
