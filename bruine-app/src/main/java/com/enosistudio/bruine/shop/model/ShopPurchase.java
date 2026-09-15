package com.enosistudio.bruine.shop.model;

import com.enosistudio.bruine.steam.model.SteamUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Trace d'un achat effectué dans la boutique. On stocke une copie (snapshot) des infos du pack
 * au moment de l'achat, pour que l'historique reste lisible même si le pack est modifié ou supprimé.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "shop_purchase")
public class ShopPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "steam_user_id", nullable = false)
    private SteamUser steamUser;

    @Column(name = "pack_name", nullable = false)
    private String packName;

    @Column(name = "pack_emoji", nullable = false)
    private String packEmoji;

    /**
     * Points réellement crédités (base + bonus).
     */
    @Column(name = "points_credited", nullable = false)
    private int pointsCredited;

    /**
     * Montant payé en centimes (promo déduite).
     */
    @Column(name = "price_cents_paid", nullable = false)
    private int priceCentsPaid;

    /**
     * Réduction promo appliquée au moment de l'achat (0 = aucune).
     */
    @Column(name = "promo_percent", nullable = false)
    private int promoPercent;

    /**
     * Session Stripe Checkout à l'origine de l'achat (garde-fou d'idempotence).
     */
    @Column(name = "stripe_session_id", unique = true)
    private String stripeSessionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getPricePaidEuros() {
        return String.format(Locale.US, "%.2f", priceCentsPaid / 100.0);
    }
}
