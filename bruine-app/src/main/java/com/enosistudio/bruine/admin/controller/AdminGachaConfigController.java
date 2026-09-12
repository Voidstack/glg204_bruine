package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.gacha.model.GachaConfig;
import com.enosistudio.bruine.gacha.repository.GachaConfigRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/gacha-config")
public class AdminGachaConfigController {

    private final GachaConfigRepository gachaConfigRepository;

    public AdminGachaConfigController(GachaConfigRepository gachaConfigRepository) {
        this.gachaConfigRepository = gachaConfigRepository;
    }

    @GetMapping
    public String page(Model model) {
        GachaConfig config = gachaConfigRepository.findById(1).orElseGet(GachaConfig::new);
        model.addAttribute("gachaConfig", config);
        return "admin/gacha-config";
    }

    @PostMapping
    public String save(
            @RequestParam int rarityLegendary,
            @RequestParam int rarityEpic,
            @RequestParam int rarityRare,
            @RequestParam int rarityUncommon,
            @RequestParam int rarityCommon,
            @RequestParam int finishNegative,
            @RequestParam int finishPolychrome,
            @RequestParam int finishFoil,
            @RequestParam int finishHolographic,
            @RequestParam int finishNormal,
            @RequestParam int xpBaseCommon,
            @RequestParam int xpBaseUncommon,
            @RequestParam int xpBaseRare,
            @RequestParam int xpBaseEpic,
            @RequestParam int xpBaseLegendary,
            @RequestParam int xpMultNormal,
            @RequestParam int xpMultHolographic,
            @RequestParam int xpMultFoil,
            @RequestParam int xpMultNegative,
            @RequestParam int xpMultPolychrome,
            RedirectAttributes redirectAttributes) {

        GachaConfig config = gachaConfigRepository.findById(1).orElseGet(GachaConfig::new);
        config.setId(1);
        config.setRarityLegendary(Math.max(rarityLegendary, 0));
        config.setRarityEpic(Math.max(rarityEpic, 0));
        config.setRarityRare(Math.max(rarityRare, 0));
        config.setRarityUncommon(Math.max(rarityUncommon, 0));
        config.setRarityCommon(Math.max(rarityCommon, 0));
        config.setFinishNegative(Math.max(finishNegative, 0));
        config.setFinishPolychrome(Math.max(finishPolychrome, 0));
        config.setFinishFoil(Math.max(finishFoil, 0));
        config.setFinishHolographic(Math.max(finishHolographic, 0));
        config.setFinishNormal(Math.max(finishNormal, 0));
        config.setXpBaseCommon(Math.max(xpBaseCommon, 0));
        config.setXpBaseUncommon(Math.max(xpBaseUncommon, 0));
        config.setXpBaseRare(Math.max(xpBaseRare, 0));
        config.setXpBaseEpic(Math.max(xpBaseEpic, 0));
        config.setXpBaseLegendary(Math.max(xpBaseLegendary, 0));
        config.setXpMultNormal(Math.max(xpMultNormal, 1));
        config.setXpMultHolographic(Math.max(xpMultHolographic, 1));
        config.setXpMultFoil(Math.max(xpMultFoil, 1));
        config.setXpMultNegative(Math.max(xpMultNegative, 1));
        config.setXpMultPolychrome(Math.max(xpMultPolychrome, 1));
        gachaConfigRepository.save(config);

        redirectAttributes.addFlashAttribute("success", "Configuration sauvegardée.");
        return "redirect:/admin/gacha-config";
    }
}
