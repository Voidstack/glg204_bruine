package com.enosistudio.bruine;

import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    private final SteamUserService steamUserService;
    private final GachaService gachaService;

    public HomeController(SteamUserService steamUserService, GachaService gachaService) {
        this.steamUserService = steamUserService;
        this.gachaService = gachaService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("statUsers", steamUserService.countPlayers());
        model.addAttribute("statPulls", steamUserService.sumTotalPulls());
        model.addAttribute("statXp", steamUserService.sumTotalExperience());
        model.addAttribute("gachaConfig", gachaService.currentConfig());
        return "index";
    }
}
