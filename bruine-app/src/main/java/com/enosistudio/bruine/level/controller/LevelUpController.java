package com.enosistudio.bruine.level.controller;

import com.enosistudio.bruine.level.dto.ConvertRequestDTO;
import com.enosistudio.bruine.level.dto.ConvertResultDTO;
import com.enosistudio.bruine.level.dto.ConvertibleCardDTO;
import com.enosistudio.bruine.level.service.LevelUpService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Page de conversion des cartes en expérience. Le calcul vit dans {@link LevelUpService}.
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
        SteamUser user = currentSteamUser.requireWithRewards();

        List<ConvertibleCardDTO> cards = levelUpService.findConvertibleCards(user);
        long totalCards = cards.stream().mapToLong(ConvertibleCardDTO::count).sum();

        ModelAndView mav = new ModelAndView("levelup/levelup");
        mav.addObject("cards", cards);
        mav.addObject("totalCards", totalCards);
        mav.addObject("totalExperience", user.getTotalExperience());
        return mav;
    }

    /**
     * Le joueur est chargé sans sa collection : la conversion supprime des cartes, et
     * Hibernate tenterait sinon de fusionner une collection contenant des lignes détruites.
     */
    @PostMapping("/convert")
    @ResponseBody
    public ResponseEntity<ConvertResultDTO> convert(@RequestBody List<ConvertRequestDTO> items) {
        SteamUser user = currentSteamUser.require();
        return ResponseEntity.ok(levelUpService.convert(user, items));
    }
}
