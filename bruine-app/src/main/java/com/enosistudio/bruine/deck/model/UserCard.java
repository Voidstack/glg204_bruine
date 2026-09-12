package com.enosistudio.bruine.deck.model;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.steam.model.SteamUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * L'entité représentant un exemplaire de carte possédé par un joueur/user.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_card")
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steam_user_id", nullable = false)
    private SteamUser steamUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gacha_reward_id", nullable = false)
    private GachaReward gachaReward;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ECardFinish finish = ECardFinish.NORMAL;

    /** Deck dans lequel cet exemplaire est posé, ou null s'il est hors deck. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;
}