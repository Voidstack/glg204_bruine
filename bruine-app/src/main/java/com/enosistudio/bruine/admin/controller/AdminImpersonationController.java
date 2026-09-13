package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.security.SteamUserPrincipal;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Controller
@RequestMapping("/admin")
public class AdminImpersonationController {

    private final SteamUserService steamUserService;
    private final SteamService steamService;

    public AdminImpersonationController(SteamUserService steamUserService, SteamService steamService) {
        this.steamUserService = steamUserService;
        this.steamService = steamService;
    }

    @PostMapping("/impersonate/{id}")
    public String impersonate(@PathVariable Long id, HttpSession session) {
        SteamUser user = steamUserService.findById(id).orElse(null);
        if (user == null) return "redirect:/admin";

        SteamUserPrincipal principal = SteamUserPrincipal.create(user, avatarUrl(user.getSteamId()));
        SteamAuthenticationToken token = new SteamAuthenticationToken(
                user.getSteamId(), principal, principal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/";
    }

    /**
     * Avatar du joueur incarné, pour que la barre de navigation montre bien le sien.
     * {@code null} si Steam ne répond pas : le principal retombe alors sur l'avatar générique,
     * une panne de l'API ne doit pas empêcher d'incarner un joueur.
     */
    private String avatarUrl(String steamId) {
        try {
            return (String) steamService.getUserData(steamId).get("avatar");
        } catch (SteamException steamIndisponible) {
            return null;
        }
    }
}
