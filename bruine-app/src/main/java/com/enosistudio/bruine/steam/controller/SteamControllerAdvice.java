package com.enosistudio.bruine.steam.controller;

import com.enosistudio.bruine.steam.exception.SteamSessionExpiredException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Ce que toutes les pages du site ont en commun, posé une fois pour toutes :
 * le score affiché dans la barre de navigation, et le rattrapage d'une session
 * Steam devenue orpheline.
 */
@ControllerAdvice
public class SteamControllerAdvice {

    private final CurrentSteamUser currentSteamUser;
    private final SecurityContextLogoutHandler steamLogoutHandler;

    public SteamControllerAdvice(CurrentSteamUser currentSteamUser, SecurityContextLogoutHandler steamLogoutHandler) {
        this.currentSteamUser = currentSteamUser;
        this.steamLogoutHandler = steamLogoutHandler;
    }

    /**
     * Score du joueur connecté, relu à chaque rendu : celui du jeton de session serait
     * périmé dès le premier tirage. {@code null} quand personne n'est connecté.
     */
    @ModelAttribute("currentScore")
    public Integer currentScore() {
        return currentSteamUser.find().map(SteamUser::getScore).orElse(null);
    }

    /**
     * La session pointe vers un compte Steam supprimé entre-temps : on déconnecte le joueur
     * (sans toucher à une éventuelle session admin) et on renvoie à l'accueil.
     */
    @ExceptionHandler(SteamSessionExpiredException.class)
    public String onSteamSessionExpired(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        steamLogoutHandler.logout(request, response, authentication);
        return "redirect:/";
    }
}
