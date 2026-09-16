package com.enosistudio.bruine.market.model;

import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.steam.model.SteamUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "market_listing")
public class MarketListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false, unique = true)
    private UserCard userCard;

    @Column(nullable = false)
    private int price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Une carte en vente n'est pas libre : elle ne change pas de propriétaire tant que l'annonce existe.
     */
    public SteamUser getSeller() {
        return userCard.getSteamUser();
    }
}
