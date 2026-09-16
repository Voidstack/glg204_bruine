package com.enosistudio.bruine.steam.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Jeu possédé par un joueur, lu dans {@code GetOwnedGames}. Alias et non JsonProperty : renvoyé au navigateur
 * avec les noms Java.
 */
public record SteamGameDTO(
        @JsonAlias("appid") int appId,
        String name,
        @JsonAlias("playtime_forever") int playtimeMinutes,
        @JsonAlias("img_icon_url") String iconHash) {

    /**
     * Inutile mais a garder.
     * C'est le JS qui va recupérer l'icone.
     */
    public String iconUrl() {
        if (iconHash == null || iconHash.isBlank()) return null;
        return "https://media.steampowered.com/steamcommunity/public/images/apps/" + appId + "/" + iconHash + ".jpg";
    }
}
