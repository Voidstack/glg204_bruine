package com.enosistudio.bruine.market;

import com.enosistudio.bruine.market.service.MarketService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/market")
public class MarketController {

    private final MarketService marketService;
    private final CurrentSteamUser currentSteamUser;

    public MarketController(MarketService marketService, CurrentSteamUser currentSteamUser) {
        this.marketService = marketService;
        this.currentSteamUser = currentSteamUser;
    }

    @GetMapping
    public String page(Model model) {
        SteamUser user = currentSteamUser.require();

        model.addAttribute("myInventory", marketService.findSellableCards(user.getId()));
        model.addAttribute("allListings", marketService.findOtherListings(user.getId()));
        model.addAttribute("myListings", marketService.findMyListings(user.getId()));
        return "market/market";
    }

    @PostMapping("/sell")
    public String sell(@RequestParam Long cardId,
                       @RequestParam int price,
                       RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();

        marketService.sell(user, cardId, price);
        redirectAttributes.addFlashAttribute("successMessage", "Carte mise en vente avec succès !");
        return "redirect:/market";
    }

    @PostMapping("/buy/{id}")
    public String buy(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();

        marketService.buy(user, id);
        redirectAttributes.addFlashAttribute("successMessage", "Carte achetée avec succès !");
        return "redirect:/market";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();

        marketService.cancel(user, id);
        redirectAttributes.addFlashAttribute("successMessage", "Annonce annulée, carte récupérée dans votre inventaire.");
        return "redirect:/market";
    }

}
