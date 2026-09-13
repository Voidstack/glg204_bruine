package com.enosistudio.bruine.steam.controller;

import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.steam.dto.SteamGameDTO;
import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.dto.SteamOpenidLoginDTO;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClientException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Controller
@RequestMapping("/steam")
public class SteamController {

    private static final Logger log = LoggerFactory.getLogger(SteamController.class);

    private final AuthenticationManager authenticationManager;
    private final SteamService steamService;
    private final SteamUserService steamUserService;
    private final SessionRegistry sessionRegistry;
    private final DeckService deckService;
    private final CurrentSteamUser currentSteamUser;

    public SteamController(AuthenticationManager authenticationManager, SteamService steamService,
                           SteamUserService steamUserService, SessionRegistry sessionRegistry,
                           DeckService deckService, CurrentSteamUser currentSteamUser) {
        this.authenticationManager = authenticationManager;
        this.steamService = steamService;
        this.steamUserService = steamUserService;
        this.sessionRegistry = sessionRegistry;
        this.deckService = deckService;
        this.currentSteamUser = currentSteamUser;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        return "redirect:" + steamService.buildSteamLoginUrl(baseUrl(request));
    }

    /**
     * Base publique du site, calculée de la même façon à l'aller et au retour de Steam
     * pour que le {@code return_to} de l'assertion puisse être comparé.
     */
    private static String baseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .toUriString();
    }

    @GetMapping("/login/redirect")
    public ModelAndView loginRedirect(HttpServletRequest request, @RequestParam Map<String, String> allRequestParams) {
        try {
            // construit dans le try : des paramètres OpenID absents ou malformés sont un échec de connexion, pas une 500
            SteamOpenidLoginDTO dto = new SteamOpenidLoginDTO(
                    allRequestParams.get("openid.ns"),
                    allRequestParams.get("openid.op_endpoint"),
                    allRequestParams.get("openid.claimed_id"),
                    allRequestParams.get("openid.identity"),
                    allRequestParams.get("openid.return_to"),
                    allRequestParams.get("openid.response_nonce"),
                    allRequestParams.get("openid.assoc_handle"),
                    allRequestParams.get("openid.signed"),
                    allRequestParams.get("openid.sig")
            );

            String steamUserId = steamService.validateLoginParameters(dto, baseUrl(request));
            SteamAuthenticationToken authReq = new SteamAuthenticationToken(steamUserId);
            Authentication auth = authenticationManager.authenticate(authReq);
            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(auth);
            SecurityContextHolder.setContext(sc);
            HttpSession session = request.getSession(true);
            // Protection contre la fixation de session : nouvel identifiant, attributs conservés
            // (dont le contexte admin). Pas de session.invalidate(), cf. isolation des deux chaînes.
            request.changeSessionId();
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);
            sessionRegistry.registerNewSession(session.getId(), auth.getPrincipal());

        } catch (IllegalArgumentException | ConstraintViolationException | RestClientException
                 | AuthenticationException echecOpenid) {
            log.warn("Échec de la connexion Steam", echecOpenid);
            return new ModelAndView("redirect:/steam/failed");
        }

        return new ModelAndView("redirect:/");
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
            boolean registeredOnSite = steamUserService.findBySteamId(steamId).isPresent();
            boolean isOwnProfile = currentSteamUser.steamId().filter(steamId::equals).isPresent();

            boolean hasDeck = steamUserService.findBySteamId(steamId)
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
    public String deleteMyAccount(@PathVariable String steamId, HttpServletRequest request) {
        // On ne supprime que son propre compte, jamais celui d'un autre joueur.
        if (currentSteamUser.steamId().filter(steamId::equals).isEmpty()) {
            return "redirect:/steam/failed";
        }
        steamUserService.findBySteamId(steamId)
                .ifPresent(user -> steamUserService.deleteById(user.getId()));
        HttpSession session = request.getSession(false);
        if (session != null) {
            // on ne fait pas session.invalidate() pour éviter déco l'admin en même temps que le steamUser
            session.removeAttribute(SPRING_SECURITY_CONTEXT_KEY);
        }
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SPRING_SECURITY_CONTEXT_KEY);
        }
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @GetMapping("/failed")
    public String failed() {
        return "steam/failed";
    }
}