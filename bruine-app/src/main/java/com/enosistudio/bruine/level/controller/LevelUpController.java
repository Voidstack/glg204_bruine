package com.enosistudio.bruine.level.controller;

import com.enosistudio.bruine.level.service.LevelUpService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Page de conversion des cartes en expérience.
 */
@Controller
@RequestMapping("/levelup")
public class LevelUpController {

    private final CurrentSteamUser currentSteamUser;
    private final LevelUpService levelUpService;

    public LevelUpController(CurrentSteamUser currentSteamUser, LevelUpService levelUpService) {
        this.currentSteamUser = currentSteamUser;
        this.levelUpService = levelUpService;
    }

    @GetMapping
    public String levelUpPage(Model model) {
        SteamUser user = currentSteamUser.require();

        model.addAttribute("cards", levelUpService.findConvertibleCards(user.getId()));
        model.addAttribute("totalExperience", user.getTotalExperience());
        return "levelup/levelup";
    }

    @PostMapping("/convert")
    public String convert(@RequestParam List<Long> cardIds, RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();
        redirectAttributes.addFlashAttribute("successMessage",
                "+" + levelUpService.convert(user, cardIds) + " XP gagnés !");
        return "redirect:/levelup";
    }
}
