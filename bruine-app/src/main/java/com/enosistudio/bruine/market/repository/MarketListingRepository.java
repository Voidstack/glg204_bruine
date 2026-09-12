package com.enosistudio.bruine.market.repository;

import com.enosistudio.bruine.market.model.MarketListing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {

    /**
     * Annonces des autres joueurs, la plus récente d'abord.
     */
    @EntityGraph(attributePaths = {"seller", "userCard", "userCard.gachaReward"})
    List<MarketListing> findBySellerIdNotOrderByCreatedAtDesc(Long sellerId);

    /**
     * Annonces d'un vendeur, la plus récente d'abord.
     */
    @EntityGraph(attributePaths = {"seller", "userCard", "userCard.gachaReward"})
    List<MarketListing> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("SELECT ml.userCard.id FROM MarketListing ml")
    Set<Long> findAllListedCardIds();

    /**
     * Une annonce court-elle déjà pour cet exemplaire ? La colonne est unique en base,
     * mais la règle doit se voir avant l'INSERT pour être refusée proprement au joueur.
     */
    boolean existsByUserCardId(Long userCardId);
}
