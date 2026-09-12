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
        GachaConfig cfg = gachaConfigRepository.findById(1).orElseGet(GachaConfig::new);
        model.addAttribute("cfg", cfg);
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

        GachaConfig cfg = gachaConfigRepository.findById(1).orElseGet(GachaConfig::new);
        cfg.setId(1);
        cfg.setRarityLegendary(Math.max(rarityLegendary, 0));
        cfg.setRarityEpic(Math.max(rarityEpic, 0));
        cfg.setRarityRare(Math.max(rarityRare, 0));
        cfg.setRarityUncommon(Math.max(rarityUncommon, 0));
        cfg.setRarityCommon(Math.max(rarityCommon, 0));
        cfg.setFinishNegative(Math.max(finishNegative, 0));
        cfg.setFinishPolychrome(Math.max(finishPolychrome, 0));
        cfg.setFinishFoil(Math.max(finishFoil, 0));
        cfg.setFinishHolographic(Math.max(finishHolographic, 0));
        cfg.setFinishNormal(Math.max(finishNormal, 0));
        cfg.setXpBaseCommon(Math.max(xpBaseCommon, 0));
        cfg.setXpBaseUncommon(Math.max(xpBaseUncommon, 0));
        cfg.setXpBaseRare(Math.max(xpBaseRare, 0));
        cfg.setXpBaseEpic(Math.max(xpBaseEpic, 0));
        cfg.setXpBaseLegendary(Math.max(xpBaseLegendary, 0));
        cfg.setXpMultNormal(Math.max(xpMultNormal, 1));
        cfg.setXpMultHolographic(Math.max(xpMultHolographic, 1));
        cfg.setXpMultFoil(Math.max(xpMultFoil, 1));
        cfg.setXpMultNegative(Math.max(xpMultNegative, 1));
        cfg.setXpMultPolychrome(Math.max(xpMultPolychrome, 1));
        gachaConfigRepository.save(cfg);

        redirectAttributes.addFlashAttribute("success", "Configuration sauvegardée.");
        return "redirect:/admin/gacha-config";
    }
}
