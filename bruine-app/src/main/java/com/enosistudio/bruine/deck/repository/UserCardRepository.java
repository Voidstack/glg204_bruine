package com.enosistudio.bruine.deck.repository;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.UserCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    @EntityGraph(attributePaths = {"gachaReward"})
    Optional<UserCard> findFirstBySteamUserIdAndGachaRewardIdAndFinish(
            Long steamUserId, Long gachaRewardId, ECardFinish finish);

    /**
     * Cartes actuellement posées dans le deck du joueur.
     */
    @EntityGraph(attributePaths = {"gachaReward"})
    List<UserCard> findBySteamUserIdAndDeckIsNotNull(Long steamUserId);
}
