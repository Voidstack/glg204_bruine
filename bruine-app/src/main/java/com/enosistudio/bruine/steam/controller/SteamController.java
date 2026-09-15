package com.enosistudio.bruine.steam.controller;

import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.steam.dto.SteamGameDTO;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    @GetMapping("/profile/{steamId}")
    public ModelAndView profileById(@PathVariable String steamId) {
        if (!steamId.matches("7656119\\d{10}")) {
            return new ModelAndView("redirect:/steam/failed");
        }
        try {
            Map<String, Object> userData = steamService.getUserData(steamId);
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
            mav.addObject("steamData", userData);
            mav.addObject("registeredOnSite", registeredOnSite);
            mav.addObject("isOwnProfile", isOwnProfile);
            mav.addObject("hasDeck", hasDeck);
            mav.addObject("deckImageUrl", deckImageUrl);
            return mav;
        } catch (SteamException steamIndisponible) {
            return new ModelAndView("redirect:/steam/failed");
        }
    }

    @GetMapping("/profile/{steamId}/games")
    @ResponseBody
    public Map<String, Object> gamesJson(@PathVariable String steamId) {
        if (!steamId.matches("7656119\\d{10}")) {
            return Map.of("games", List.of(), "totalPlaytimeMinutes", 0L);
        }
        try {
            List<SteamGameDTO> games = steamService.getOwnedGames(steamId).stream()
                    .filter(SteamService::isPlayed)
                    .sorted(Comparator.comparingInt(SteamGameDTO::playtimeMinutes).reversed())
                    .toList();
            long total = games.stream().mapToLong(SteamGameDTO::playtimeMinutes).sum();
            return Map.of("games", games, "totalPlaytimeMinutes", total);
        } catch (SteamException steamIndisponible) {
            return Map.of("games", List.of(), "totalPlaytimeMinutes", 0L);
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