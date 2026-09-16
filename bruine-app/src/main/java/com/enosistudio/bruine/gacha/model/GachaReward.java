package com.enosistudio.bruine.gacha.model;

import com.enosistudio.bruine.card.ECardRarity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * L'entité représentant un type de récompense pouvant être obtenu via le gacha mais de facon générale, pas possédé par un joueur/user donc pas de Finition pour éviter de multiplier les entités. C'etait pas une super idée au final.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gacha_reward")
public class GachaReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String emoji;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ECardRarity rarity;

    private String description;
}
