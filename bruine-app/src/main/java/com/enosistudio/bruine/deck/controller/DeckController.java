package com.enosistudio.bruine.deck.controller;

import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.deck.DeckSvgRenderer;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/deck")
public class DeckController {

    private final CurrentSteamUser currentSteamUser;
    private final SteamUserService steamUserService;
    private final DeckService deckService;
    private final UserCardService userCardService;
    private final SteamService steamService;
    private final DeckSvgRenderer deckSvgRenderer;

    public DeckController(CurrentSteamUser currentSteamUser,
                          SteamUserService steamUserService,
                          DeckService deckService,
                          UserCardService userCardService,
                          SteamService steamService,
                          DeckSvgRenderer deckSvgRenderer) {
        this.currentSteamUser = currentSteamUser;
        this.steamUserService = steamUserService;
        this.deckService = deckService;
        this.userCardService = userCardService;
        this.steamService = steamService;
        this.deckSvgRenderer = deckSvgRenderer;
    }

    @GetMapping
    public String page(Model model) {
        SteamUser user = currentSteamUser.require();

        model.addAttribute("inventoryStacks", userCardService.findOwnedCards(user.getId()));
        model.addAttribute("deckCardIds", deckService.findDeckCardIds(user.getId()));
        return "deck/deck";
    }

    @PostMapping
    public String save(@RequestParam(required = false) List<Long> cardIds,
                       RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();

        deckService.saveDeck(user.getId(), cardIds);
        redirectAttributes.addFlashAttribute("successMessage", "Deck sauvegardé avec succès !");
        return "redirect:/deck";
    }

    /**
     * Deck d'un joueur rendu en image SVG, servi sans layout ni authentification
     * (route publique déclarée dans {@code WebSecurityConfig}). C'est la seule façon de partager un deck :
     * elle s'intègre dans une page web ou un fichier Markdown via une balise {@code <img>}.
     *
     * @return le SVG du deck, ou 404 si l'ID est invalide ou l'utilisateur inconnu
     */
    @GetMapping(value = "/{steamId:" + SteamService.STEAM_ID_PATTERN + "}", produces = "image/svg+xml;charset=UTF-8")
    @ResponseBody
    public String deckImage(@PathVariable String steamId) {
        SteamUser user = steamUserService.findBySteamId(steamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<UserCard> deckCards = deckService.findDeckCards(user.getId());

        return deckSvgRenderer.render(user, deckCards, avatarDataUri(steamId));
    }

    /**
     * Avatar Steam encodé en {@code data:} URI pour être intégré dans le SVG.
     * Une SVG chargée en {@code <img>} ne peut pas récupérer une image distante :
     * l'avatar doit donc voyager dans le fichier. {@code null} si Steam ne répond pas.
     */
    private String avatarDataUri(String steamId) {
        try {
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(steamService.getAvatarMedium(steamId));
        } catch (SteamException avatarIndisponible) {
            return null;
        }
    }
}
