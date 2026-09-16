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
import org.springframework.web.servlet.ModelAndView;
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
    public ModelAndView profile() {
        return currentSteamUser.steamId()
                .map(connecte -> new ModelAndView("redirect:/steam/profile/" + connecte))
                .orElseGet(() -> new ModelAndView("redirect:/"));
    }

    @GetMapping("/profile/{steamId:" + SteamService.STEAM_ID_PATTERN + "}")
    public ModelAndView profileById(@PathVariable String steamId) {
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

            ModelAndView mav = new ModelAndView("steam/profile-by-id");
            mav.addObject("player", player);
            mav.addObject("registeredOnSite", registeredOnSite);
            mav.addObject("isOwnProfile", isOwnProfile);
            mav.addObject("hasDeck", hasDeck);
            mav.addObject("deckImageUrl", deckImageUrl);
            return mav;
        } catch (SteamException steamIndisponible) {
            return new ModelAndView("redirect:/steam/failed");
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