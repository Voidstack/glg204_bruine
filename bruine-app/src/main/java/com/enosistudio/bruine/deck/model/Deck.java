package com.enosistudio.bruine.deck.model;

import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.steam.model.SteamUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Deck vitrine d'un joueur, chaque SteamUser en possède exactement un, créé à son inscription.
 * Il référence de 0 à 10 cartes du joueur, dans l'ordre choisi (table {@code deck_card}, colonne {@code slot}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deck")
public class Deck {

    public static final int MAX_CARDS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steam_user_id", nullable = false, unique = true)
    private SteamUser steamUser;

    @ManyToMany
    @JoinTable(name = "deck_card",
            joinColumns = @JoinColumn(name = "deck_id"),
            inverseJoinColumns = @JoinColumn(name = "user_card_id", unique = true))
    @OrderColumn(name = "slot")
    private List<UserCard> cards = new ArrayList<>();

    public Deck(SteamUser steamUser) {
        this.steamUser = steamUser;
    }
}
