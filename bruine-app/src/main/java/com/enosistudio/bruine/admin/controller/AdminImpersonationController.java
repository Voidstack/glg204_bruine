package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.security.SteamUserPrincipal;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminImpersonationController {

    /**
     * Même dépôt que la chaîne principale (clé de session par défaut), volontairement local :
     * un bean global s'appliquerait aussi aux chaînes de sécurité.
     */
    private final HttpSessionSecurityContextRepository steamContextRepository = new HttpSessionSecurityContextRepository();

    private final SteamUserService steamUserService;
    private final SteamService steamService;

    public AdminImpersonationController(SteamUserService steamUserService, SteamService steamService) {
        this.steamUserService = steamUserService;
        this.steamService = steamService;
    }

    /**
     * Ouvre une session Steam au nom du joueur. La requête passe par la chaîne admin, dont le
     * contexte courant est celui de l'administrateur : le contexte Steam est donc écrit
     * directement dans le dépôt en session de la chaîne principale, comme le fait la connexion Steam.
     */
    @PostMapping("/impersonate/{id}")
    public String impersonate(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        SteamUser user = steamUserService.findById(id).orElse(null);
        if (user == null) return "redirect:/admin";

        SteamUserPrincipal principal = SteamUserPrincipal.create(user, avatarUrl(user.getSteamId()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(SteamAuthenticationToken.authenticated(principal));

        request.changeSessionId();
        steamContextRepository.saveContext(context, request, response);

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
