package com.enosistudio.bruine.steam.controller;

import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.security.SteamSessionExpiredException;
import com.enosistudio.bruine.steam.security.SteamUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

/*
sert à centraliser la gestion des exceptions des controleurs Spring.
 */
@ControllerAdvice
public class SteamControllerAdvice {

    private final SteamUserService steamUserService;

    public SteamControllerAdvice(SteamUserService steamUserService) {
        this.steamUserService = steamUserService;
    }

    @ModelAttribute("currentScore")
    public Integer currentScore(Authentication authentication) {
        if (authentication instanceof SteamAuthenticationToken token
                && token.isAuthenticated()
                && token.getPrincipal() != null) {
            return steamUserService.findBySteamId(token.getPrincipal().steamId())
                    .map(SteamUser::getScore)
                    .orElse(null);
        }
        return null;
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
