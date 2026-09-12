package com.enosistudio.bruine.deck.repository;

import com.enosistudio.bruine.deck.model.Deck;
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
}
