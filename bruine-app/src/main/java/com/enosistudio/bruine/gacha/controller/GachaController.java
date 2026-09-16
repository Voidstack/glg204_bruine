package com.enosistudio.bruine.gacha.controller;

import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Page du gacha et point d'entrée du tirage. Les règles de jeu vivent dans {@link GachaService}.
 */
@Controller
@RequestMapping("/gacha")
public class GachaController {

    private final CurrentSteamUser currentSteamUser;
    private final GachaService gachaService;

    public GachaController(CurrentSteamUser currentSteamUser, GachaService gachaService) {
        this.currentSteamUser = currentSteamUser;
        this.gachaService = gachaService;
    }

    @GetMapping
    public ModelAndView page() {
        SteamUser user = currentSteamUser.require();

        GachaConfig config = gachaService.currentConfig();
        ModelAndView mav = new ModelAndView("gacha/gacha");
        mav.addObject("costPerPull", GachaService.COST_PER_PULL);
        mav.addObject("userScore", user.getScore());
        mav.addObject("gachaConfig", config);
        mav.addObject("rarityTotal", Math.max(config.rarityWeightTotal(), 1));
        mav.addObject("finishTotal", Math.max(config.finishWeightTotal(), 1));
        return mav;
    }

    /**
     * Effectue le tirage puis renvoie le joueur sur la page, qui affichera le résultat.
     * <p>
     * Le passage par une redirection est ce qui empêche un rafraîchissement du navigateur
     * de rejouer le tirage : le résultat voyage en attribut flash, le temps d'une requête.
     */
    @PostMapping("/spin")
    public String spin(@RequestParam(defaultValue = "1") int count, RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();
        redirectAttributes.addFlashAttribute("spinResult", gachaService.spin(user, count));
        return "redirect:/gacha"; // rechargement de la page pour afficher le résultat du tirage
    }
}
