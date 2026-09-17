package com.enosistudio.bruine.shop.repository;

import com.enosistudio.bruine.shop.model.ShopPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface ShopPackRepository extends JpaRepository<ShopPack, Long> {

    List<ShopPack> findAllByOrderBySortOrderAscIdAsc();

    /**
     * Retire le drapeau « Populaire » de tous les packs sauf celui indiqué.
     * WHERE p.id <> :id veut dire si p.id != id en param
     */
    @Modifying
    @Query("UPDATE ShopPack p SET p.popular = false WHERE p.id <> :id")
    void clearPopularExcept(@Param("id") Long id);
}
