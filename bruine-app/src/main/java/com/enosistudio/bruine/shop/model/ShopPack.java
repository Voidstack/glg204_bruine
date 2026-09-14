package com.enosistudio.bruine.shop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Pack achetable dans la boutique : un prix en euros crédite un certain nombre de points Bruine.
 * Géré entièrement depuis le back-office admin (CRUD + promos temporaires + drapeau « Populaire »).
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shop_pack")
public class ShopPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String emoji;

    /**
     * Points de base crédités à l'achat.
     */
    @Column(nullable = false)
    private int points;

    /**
     * Points offerts en bonus (0 = aucun).
     */
    @Column(name = "bonus_points", nullable = false)
    private int bonusPoints;

    /**
     * Prix en centimes d'euro (évite les erreurs d'arrondi des flottants).
     */
    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    /**
     * Un seul pack à la fois est mis en avant (« Populaire »).
     */
    @Column(nullable = false)
    private boolean popular;

    /**
     * Ordre d'affichage dans la boutique.
     */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * Réduction en pourcentage (0 = pas de promo).
     */
    @Column(name = "promo_percent", nullable = false)
    private int promoPercent;

    /**
     * Début de la promo (null = pas de borne inférieure).
     */
    @Column(name = "promo_start")
    private LocalDateTime promoStart;

    /**
     * Fin de la promo (null = pas de borne supérieure).
     */
    @Column(name = "promo_end")
    private LocalDateTime promoEnd;

    // Helpers d'affichage (non persistés)

    /**
     * Total de points réellement crédités (base + bonus).
     */
    @Transient
    public int getTotalPoints() {
        return points + bonusPoints;
    }

    /**
     * La promo est-elle active à l'instant présent ?
     */
    @Transient
    public boolean isPromoActive() {
        if (promoPercent <= 0) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (promoStart != null && now.isBefore(promoStart)) {
            return false;
        }
        return promoEnd == null || !now.isAfter(promoEnd);
    }

    /**
     * Prix effectivement payé, promo éventuelle déduite.
     */
    @Transient
    public int getEffectivePriceCents() {
        if (!isPromoActive()) {
            return priceCents;
        }
        return (int) Math.round(priceCents * (100.0 - promoPercent) / 100.0);
    }

    /**
     * Prix catalogue formaté « 4.99 ».
     */
    @Transient
    public String getPriceEuros() {
        return String.format(Locale.US, "%.2f", priceCents / 100.0);
    }

    /**
     * Prix effectif formaté « 3.99 » (après promo).
     */
    @Transient
    public String getEffectivePriceEuros() {
        return String.format(Locale.US, "%.2f", getEffectivePriceCents() / 100.0);
    }
}
