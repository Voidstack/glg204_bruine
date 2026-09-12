package com.enosistudio.bruine.deck.model;

import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.steam.model.SteamUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ancrage du deck vitrine d'un joueur au plus un par {@link SteamUser}.
 * Le deck ne porte pas la liste de ses cartes : c'est chaque {@link UserCard} qui pointe
 * vers son deck via sa propre colonne {@code deck_id}, ou vers rien si elle est hors
 * deck. Une carte n'appartenant qu'à un seul deck à la fois, une table de jointure N-N
 * n'aurait aucun sens ici.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deck")
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steam_user_id", nullable = false, unique = true)
    private SteamUser steamUser;
}
