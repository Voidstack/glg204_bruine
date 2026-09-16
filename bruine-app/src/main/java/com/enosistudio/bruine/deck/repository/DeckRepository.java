package com.enosistudio.bruine.deck.repository;

import com.enosistudio.bruine.deck.model.Deck;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {

    Optional<Deck> findBySteamUserId(Long steamUserId);

    /**
     * Deck du joueur avec ses cartes et leur récompense déjà chargées, pour l'affichage.
     */
    @EntityGraph(attributePaths = {"cards", "cards.gachaReward"})
    Optional<Deck> findWithCardsBySteamUserId(Long steamUserId);

    boolean existsByCardsId(Long userCardId);
}
