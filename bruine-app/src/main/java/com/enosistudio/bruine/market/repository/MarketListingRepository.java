package com.enosistudio.bruine.market.repository;

import com.enosistudio.bruine.market.model.MarketListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {

    @Query("SELECT ml FROM MarketListing ml JOIN FETCH ml.seller JOIN FETCH ml.userCard c JOIN FETCH c.gachaReward WHERE ml.seller.id <> :sellerId ORDER BY ml.createdAt DESC")
    List<MarketListing> findOthersWithDetails(@Param("sellerId") Long sellerId);

    @Query("SELECT ml FROM MarketListing ml JOIN FETCH ml.seller JOIN FETCH ml.userCard c JOIN FETCH c.gachaReward WHERE ml.seller.id = :sellerId ORDER BY ml.createdAt DESC")
    List<MarketListing> findBySellerIdWithDetails(@Param("sellerId") Long sellerId);

    @Query("SELECT ml.userCard.id FROM MarketListing ml")
    Set<Long> findAllListedCardIds();
}
