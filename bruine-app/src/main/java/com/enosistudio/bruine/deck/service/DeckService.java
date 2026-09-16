package com.enosistudio.bruine.deck.service;

import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserCardService userCardService;

    public DeckService(DeckRepository deckRepository, UserCardService userCardService) {
        this.deckRepository = deckRepository;
        this.userCardService = userCardService;
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
     * Identifiants des cartes du deck du joueur, dans l'ordre choisi.
     */
    @Transactional(readOnly = true)
    public List<Long> findDeckCardIds(Long userId) {
        return findDeckCards(userId).stream()
                .map(UserCard::getId)
                .toList();
    }

    /**
     * Remplace le contenu du deck par les cartes sélectionnées, dans l'ordre reçu.
     * Ne sont retenues que les 10 premières cartes du joueur qui ne sont pas en vente.
     */
    @Transactional
    public void saveDeck(Long userId, List<Long> cardIds) {
        Deck deck = deckRepository.findBySteamUserId(userId).orElseThrow();
        Map<Long, UserCard> selectable = userCardService.findNotListed(userId).stream()
                .collect(Collectors.toMap(UserCard::getId, Function.identity()));

        List<UserCard> cards = cardIds == null ? List.of() : cardIds.stream()
                .distinct()
                .map(selectable::get)
                .filter(Objects::nonNull)
                .limit(Deck.MAX_CARDS)
                .toList();

        // Nouvelle liste plutôt que clear() : Hibernate supprime les anciennes lignes de deck_card avant
        // d'insérer les nouvelles, sans conflit sur l'unicité d'une carte quand l'ordre change.
        deck.setCards(new ArrayList<>(cards));
    }
}
