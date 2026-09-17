package com.enosistudio.bruine.inventory.controller;

import com.enosistudio.bruine.card.dto.CardStackDTO;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.gacha.service.GachaRewardService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Inventaire du joueur : liste plate de ses cartes et progression de collection.
 */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final CurrentSteamUser currentSteamUser;
    private final UserCardService userCardService;
    private final GachaRewardService gachaRewardService;

    public InventoryController(CurrentSteamUser currentSteamUser,
                               UserCardService userCardService,
                               GachaRewardService gachaRewardService) {
        this.currentSteamUser = currentSteamUser;
        this.userCardService = userCardService;
        this.gachaRewardService = gachaRewardService;
    }

    @GetMapping
    public String inventory(Model model) {
        SteamUser user = currentSteamUser.require();

        List<CardStackDTO> cards = userCardService.findOwnedCards(user.getId());

        model.addAttribute("cards", cards);
        model.addAttribute("uniqueCount", userCardService.countDistinctRewards(cards));
        model.addAttribute("totalPulls", userCardService.countOwned(cards));
        model.addAttribute("totalAvailable", gachaRewardService.findAll().size());
        return "inventory/inventory";
    }
}
