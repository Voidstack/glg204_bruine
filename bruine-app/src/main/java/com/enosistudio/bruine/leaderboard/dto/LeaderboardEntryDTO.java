package com.enosistudio.bruine.leaderboard.dto;

import com.enosistudio.bruine.steam.model.SteamUser;

import java.time.LocalDateTime;

/**
 * Ligne du classement, statistique déjà mise en forme ; {@code stat} vaut null si le temps de jeu est privé.
 */
public record LeaderboardEntryDTO(String steamId, String username, String stat, LocalDateTime createdAt) {

    public static LeaderboardEntryDTO playtime(SteamUser user) {
        Long minutes = user.getCurrentPlaytimeMinutes();
        String stat = minutes == null ? null : (minutes / 60) + "h " + (minutes % 60) + "min";
        return new LeaderboardEntryDTO(user.getSteamId(), user.getUsername(), stat, user.getCreatedAt());
    }

    public static LeaderboardEntryDTO xp(SteamUser user) {
        return new LeaderboardEntryDTO(user.getSteamId(), user.getUsername(), user.getTotalExperience() + " XP",
                user.getCreatedAt());
    }
}
