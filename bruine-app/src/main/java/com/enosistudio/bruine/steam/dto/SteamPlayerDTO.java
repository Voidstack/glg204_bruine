package com.enosistudio.bruine.steam.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Profil d'un joueur, lu dans {@code GetPlayerSummaries}. */
public record SteamPlayerDTO(
        @JsonAlias("steamid") String steamId,
        @JsonAlias("personaname") String personaName,
        @JsonAlias("realname") String realName,
        @JsonAlias("avatarmedium") String avatarMedium,
        @JsonAlias("avatarfull") String avatarFull,
        @JsonAlias("personastate") int personaState,
        @JsonAlias("communityvisibilitystate") int communityVisibilityState,
        @JsonAlias("loccountrycode") String countryCode,
        @JsonAlias("profileurl") String profileUrl) {
}
