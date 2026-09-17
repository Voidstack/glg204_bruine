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
import org.springframework.ui.Model;
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
    public String page(Model model) {
        SteamUser user = currentSteamUser.require();

        GachaConfig config = gachaService.currentConfig();
        model.addAttribute("costPerPull", GachaService.COST_PER_PULL);
        model.addAttribute("userScore", user.getScore());
        model.addAttribute("gachaConfig", config);
        model.addAttribute("rarityTotal", Math.max(config.rarityWeightTotal(), 1));
        model.addAttribute("finishTotal", Math.max(config.finishWeightTotal(), 1));
        return "gacha/gacha";
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
