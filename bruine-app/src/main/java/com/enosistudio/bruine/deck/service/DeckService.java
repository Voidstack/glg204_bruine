package com.enosistudio.bruine.deck.service;

import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeckService {

    private static final int MAX_DECK_SIZE = 10;

    private final DeckRepository deckRepository;
    private final SteamUserRepository steamUserRepository;
    private final UserCardRepository userCardRepository;
    private final MarketListingRepository marketListingRepository;

    public DeckService(DeckRepository deckRepository,
                       SteamUserRepository steamUserRepository,
                       UserCardRepository userCardRepository,
                       MarketListingRepository marketListingRepository) {
        this.deckRepository = deckRepository;
        this.steamUserRepository = steamUserRepository;
        this.userCardRepository = userCardRepository;
        this.marketListingRepository = marketListingRepository;
    }

    /**
     * Retourne les cartes actuellement dans le deck du user.
     */
    @Transactional(readOnly = true)
    public List<UserCard> findDeckCards(Long userId) {
        return userCardRepository.findBySteamUserIdAndDeckIsNotNull(userId);
    }

    /**
     * Retourne les IDs des cartes actuellement dans le deck du user.
     */
    @Transactional(readOnly = true)
    public Set<Long> findDeckCardIds(Long userId) {
        return findDeckCards(userId).stream()
                .map(UserCard::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Crée ou met à jour le deck avec les cartes sélectionnées.
     * Vérifie que chaque carte appartient bien à l'utilisateur et n'est pas en vente.
     */
    @Transactional
    public void saveDeck(Long userId, List<Long> cardIds) {
        SteamUser user = steamUserRepository.findById(userId).orElseThrow();

        Deck deck = deckRepository.findBySteamUserId(userId).orElseGet(() -> {
            Deck d = new Deck();
            d.setSteamUser(user);
            return deckRepository.save(d);
        });

        // Détache d'abord tout ce qui était dans le deck : la sélection est recomposée
        // en entier à chaque sauvegarde, pas fusionnée avec l'ancienne.
        userCardRepository.clearDeck(deck.getId());

        if (cardIds != null) {
            Set<Long> listedCardIds = marketListingRepository.findAllListedCardIds();
            cardIds.stream()
                    .limit(MAX_DECK_SIZE)
                    .distinct()
                    .map(id -> userCardRepository.findById(id).orElse(null))
                    .filter(card -> card != null
                            && card.getSteamUser().getId().equals(userId)
                            && !listedCardIds.contains(card.getId()))
                    .forEach(card -> card.setDeck(deck));
            // Chaque carte est une entité gérée par la transaction en cours : poser son
            // deck suffit, Hibernate écrit la mise à jour au flush sans save() explicite.
        }
    }
}
