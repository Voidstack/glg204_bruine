package com.enosistudio.bruine.level.controller;

import com.enosistudio.bruine.level.dto.ConvertFormDTO;
import com.enosistudio.bruine.level.dto.ConvertibleCardDTO;
import com.enosistudio.bruine.level.service.LevelUpService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
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
    public ModelAndView levelUpPage() {
        SteamUser user = currentSteamUser.require();

        List<ConvertibleCardDTO> cards = levelUpService.findConvertibleCards(user.getId());
        long totalCards = cards.stream().mapToLong(ConvertibleCardDTO::count).sum();

        ModelAndView mav = new ModelAndView("levelup/levelup");
        mav.addObject("cards", cards);
        mav.addObject("totalCards", totalCards);
        mav.addObject("totalExperience", user.getTotalExperience());
        return mav;
    }

    @PostMapping("/convert")
    public String convert(@ModelAttribute ConvertFormDTO form, RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();
        redirectAttributes.addFlashAttribute("successMessage",
                "+" + levelUpService.convert(user, form.items()) + " XP gagnés !");
        return "redirect:/levelup";
    }
}
