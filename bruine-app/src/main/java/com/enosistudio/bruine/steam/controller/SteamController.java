package com.enosistudio.bruine.steam.controller;

import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.steam.dto.SteamGameDTO;
import com.enosistudio.bruine.steam.dto.SteamPlayerDTO;
import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import com.enosistudio.bruine.steam.security.SteamOpenIdAuthenticationFilter;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Pages Steam. Le retour de connexion ({@code /steam/login/redirect}) est traité par
 * {@link SteamOpenIdAuthenticationFilter}, la déconnexion ({@code /steam/logout}) par le
 * {@code .logout()} de la chaîne de sécurité.
 */
@Controller
@RequestMapping("/steam")
public class SteamController {

    private final SteamService steamService;
    private final SteamUserService steamUserService;
    private final DeckService deckService;
    private final CurrentSteamUser currentSteamUser;
    private final SecurityContextLogoutHandler steamLogoutHandler;

    public SteamController(SteamService steamService, SteamUserService steamUserService,
                           DeckService deckService, CurrentSteamUser currentSteamUser,
                           SecurityContextLogoutHandler steamLogoutHandler) {
        this.steamService = steamService;
        this.steamUserService = steamUserService;
        this.deckService = deckService;
        this.currentSteamUser = currentSteamUser;
        this.steamLogoutHandler = steamLogoutHandler;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        return "redirect:" + steamService.buildSteamLoginUrl(SteamOpenIdAuthenticationFilter.baseUrl(request));
    }

    @GetMapping("/profile")
    public String profile() {
        return currentSteamUser.steamId()
                .map(connecte -> "redirect:/steam/profile/" + connecte)
                .orElse("redirect:/");
    }

    @GetMapping("/profile/{steamId:" + SteamService.STEAM_ID_PATTERN + "}")
    public String profileById(@PathVariable String steamId, Model model) {
        try {
            SteamPlayerDTO player = steamService.getPlayer(steamId);
            Optional<SteamUser> registeredUser = steamUserService.findBySteamId(steamId);
            boolean registeredOnSite = registeredUser.isPresent();
            boolean isOwnProfile = currentSteamUser.steamId().filter(steamId::equals).isPresent();

            boolean hasDeck = registeredUser
                    .map(u -> !deckService.findDeckCardIds(u.getId()).isEmpty())
                    .orElse(false);

            // URL absolue de l'image SVG du deck, pour l'affichage et le code de partage
            String deckImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/deck/").path(steamId).toUriString();

            model.addAttribute("player", player);
            model.addAttribute("registeredOnSite", registeredOnSite);
            model.addAttribute("isOwnProfile", isOwnProfile);
            model.addAttribute("hasDeck", hasDeck);
            model.addAttribute("deckImageUrl", deckImageUrl);
            return "steam/profile-by-id";
        } catch (SteamException steamIndisponible) {
            return "redirect:/steam/failed";
        }
    }

    @GetMapping("/profile/{steamId:" + SteamService.STEAM_ID_PATTERN + "}/games")
    @ResponseBody
    public List<SteamGameDTO> gamesJson(@PathVariable String steamId) {
        try {
            return steamService.getPlayedGames(steamId);
        } catch (SteamException profilPriveOuSteamMuet) {
            return List.of();
        }
    }

    @PostMapping("/profile/{steamId}/delete")
    public String deleteMyAccount(@PathVariable String steamId, HttpServletRequest request,
                                  HttpServletResponse response, Authentication authentication) {
        // On ne supprime que son propre compte, jamais celui d'un autre joueur.
        if (currentSteamUser.steamId().filter(steamId::equals).isEmpty()) {
            return "redirect:/steam/failed";
        }
        steamUserService.findBySteamId(steamId)
                .ifPresent(user -> steamUserService.deleteById(user.getId()));
        steamLogoutHandler.logout(request, response, authentication);
        return "redirect:/";
    }

    @GetMapping("/failed")
    public String failed() {
        return "steam/failed";
    }
}