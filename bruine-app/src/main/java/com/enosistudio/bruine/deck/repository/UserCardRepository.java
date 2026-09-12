package com.enosistudio.bruine.deck.repository;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.steam.model.SteamUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    List<UserCard> findBySteamUserIdAndGachaRewardId(Long steamUserId, Long gachaRewardId);

    @EntityGraph(attributePaths = {"gachaReward"})
    Optional<UserCard> findFirstBySteamUserIdAndGachaRewardIdAndFinish(
            Long steamUserId, Long gachaRewardId, ECardFinish finish);

    /**
     * Cartes actuellement posées dans le deck du joueur.
     */
    @EntityGraph(attributePaths = {"gachaReward"})
    List<UserCard> findBySteamUserIdAndDeckIsNotNull(Long steamUserId);

    @Modifying
    @Transactional
    @Query("UPDATE UserCard c SET c.steamUser = :newOwner WHERE c.id = :cardId")
    void transferToNewOwner(@Param("cardId") Long cardId, @Param("newOwner") SteamUser newOwner);

    /**
     * Détache toutes les cartes d'un deck, sans les détruire, le deck redevient vide.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserCard c SET c.deck = null WHERE c.deck.id = :deckId")
    void clearDeck(@Param("deckId") Long deckId);
}
