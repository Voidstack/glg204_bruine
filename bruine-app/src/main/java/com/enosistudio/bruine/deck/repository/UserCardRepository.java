package com.enosistudio.bruine.deck.repository;

import com.enosistudio.bruine.card.UserCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    /**
     * Toutes les cartes du joueur avec leur récompense, dans l'ordre d'obtention.
     */
    @EntityGraph(attributePaths = {"gachaReward"})
    List<UserCard> findBySteamUserIdOrderById(Long steamUserId);
}
