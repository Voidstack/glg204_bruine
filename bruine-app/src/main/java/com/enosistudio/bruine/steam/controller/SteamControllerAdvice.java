package com.enosistudio.bruine.steam.controller;

import com.enosistudio.bruine.steam.exception.SteamSessionExpiredException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

/**
 * Ce que toutes les pages du site ont en commun, posé une fois pour toutes :
 * le score affiché dans la barre de navigation, et le rattrapage d'une session
 * Steam devenue orpheline.
 */
@ControllerAdvice
public class SteamControllerAdvice {

    private final CurrentSteamUser currentSteamUser;

    public SteamControllerAdvice(CurrentSteamUser currentSteamUser) {
        this.currentSteamUser = currentSteamUser;
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
     * La session pointe vers un compte Steam supprimé entre-temps : on retire l'authentification
     * Steam (sans toucher à une éventuelle session admin) et on renvoie à l'accueil.
     */
    @ExceptionHandler(SteamSessionExpiredException.class)
    public String onSteamSessionExpired(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SPRING_SECURITY_CONTEXT_KEY);
        }
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }
}
