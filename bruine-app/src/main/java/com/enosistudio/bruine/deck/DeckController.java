package com.enosistudio.bruine.deck;

import com.enosistudio.bruine.card.UserCard;
import com.enosistudio.bruine.card.UserCardService;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

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
    public ModelAndView page() {
        SteamUser user = currentSteamUser.requireWithRewards();

        ModelAndView mav = new ModelAndView("deck/deck");
        mav.addObject("inventoryStacks", userCardService.findOwnedCards(user));
        mav.addObject("deckCardIds", deckService.findDeckCardIds(user.getId()));
        return mav;
    }

    @PostMapping
    public String save(@RequestParam(required = false) List<Long> cardIds) {
        SteamUser user = currentSteamUser.requireWithRewards();

        deckService.saveDeck(user.getId(), cardIds);
        return "redirect:/deck";
    }

    /**
     * Deck d'un joueur rendu en image SVG, servi sans layout ni authentification
     * (chaîne {@code deckWidgetFilterChain}). C'est la seule façon de partager un deck :
     * elle s'intègre dans une page web ou un fichier Markdown via une balise {@code <img>}.
     *
     * @param steamId ID Steam (17 chiffres, format {@code 7656119...})
     * @return le SVG du deck, ou 404 si l'ID est invalide ou l'utilisateur inconnu
     */
    @GetMapping(value = "/{steamId}", produces = "image/svg+xml;charset=UTF-8")
    @ResponseBody
    public String deckImage(@PathVariable String steamId) {
        if (!steamId.matches("7656119\\d{10}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

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
            Object url = steamService.getUserData(steamId).get("avatarmedium");
            if (!(url instanceof String s) || s.isBlank()) return null;
            byte[] bytes = new RestTemplate().getForObject(s, byte[].class);
            if (bytes == null || bytes.length == 0) return null;
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
