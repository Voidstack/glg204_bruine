package com.enosistudio.bruine.gacha.repository;

import com.enosistudio.bruine.gacha.model.GachaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
public interface GachaConfigRepository extends JpaRepository<GachaConfig, Integer> {
}
