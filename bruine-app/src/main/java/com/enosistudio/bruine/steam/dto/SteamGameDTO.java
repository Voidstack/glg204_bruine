package com.enosistudio.bruine.steam.dto;

public record SteamGameDTO(int appId, String name, int playtimeMinutes, String iconHash) {

    public String iconUrl() {
        if (iconHash == null || iconHash.isBlank()) return null;
        return "https://media.steampowered.com/steamcommunity/public/images/apps/" + appId + "/" + iconHash + ".jpg";
    }
}
