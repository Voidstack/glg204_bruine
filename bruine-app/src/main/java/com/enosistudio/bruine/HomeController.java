package com.enosistudio.bruine;

import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.repository.GachaConfigRepository;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    private final SteamUserRepository steamUserRepository;
    private final GachaConfigRepository gachaConfigRepository;

    public HomeController(SteamUserRepository steamUserRepository, GachaConfigRepository gachaConfigRepository) {
        this.steamUserRepository = steamUserRepository;
        this.gachaConfigRepository = gachaConfigRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("statUsers", steamUserRepository.count());
        model.addAttribute("statPulls", nullToZero(steamUserRepository.sumTotalPulls()));
        model.addAttribute("statXp", nullToZero(steamUserRepository.sumTotalExperience()));
        model.addAttribute("gachaCfg", gachaConfigRepository.findById(1).orElseGet(GachaConfig::new));
        return "index";
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }
}
