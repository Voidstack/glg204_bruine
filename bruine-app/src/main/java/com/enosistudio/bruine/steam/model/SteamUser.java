package com.enosistudio.bruine.steam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "steam_user")
public class SteamUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_id", nullable = false, unique = true)
    private String steamId;

    @Column(nullable = false)
    private String username;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "score", nullable = false)
    private int score = 0;

    @Column(name = "total_experience", nullable = false)
    private long totalExperience = 0;

    @Column(name = "total_pulls", nullable = false)
    private long totalPulls = 0;

    /**
     * Temps de jeu total Steam au moment de l'inscription (en minutes). Jamais modifié après.
     */
    @Column(name = "initial_playtime_minutes")
    private Long initialPlaytimeMinutes;

    /**
     * Temps de jeu total Steam mis à jour à chaque connexion (en minutes).
     */
    @Column(name = "current_playtime_minutes")
    private Long currentPlaytimeMinutes;

    public SteamUser() {
    }

    public SteamUser(Long id, String steamId, String username) {
        this.id = id;
        this.steamId = steamId;
        this.username = username;
    }
}