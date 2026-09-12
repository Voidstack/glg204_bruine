package com.enosistudio.bruine.gacha.repository;

import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.gacha.model.GachaReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface GachaRewardRepository extends JpaRepository<GachaReward, Long> {

    List<GachaReward> findByRarity(ECardRarity rarity);
}
