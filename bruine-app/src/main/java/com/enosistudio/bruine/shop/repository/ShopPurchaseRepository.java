package com.enosistudio.bruine.shop.repository;

import com.enosistudio.bruine.shop.model.ShopPurchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopPurchaseRepository extends JpaRepository<ShopPurchase, Long> {

    /** Historique d'un utilisateur, du plus récent au plus ancien. */
    @EntityGraph(attributePaths = {"steamUser"})
    List<ShopPurchase> findBySteamUser_IdOrderByCreatedAtDesc(Long steamUserId);

    /** Tous les achats (vue admin), avec l'utilisateur chargé pour éviter les LazyInit. */
    @EntityGraph(attributePaths = {"steamUser"})
    List<ShopPurchase> findAllByOrderByCreatedAtDesc();

    /** Un achat a-t-il déjà été enregistré pour cette session Stripe ? (idempotence) */
    boolean existsByStripeSessionId(String stripeSessionId);
}
