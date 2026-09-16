package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.service.GachaRewardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/gacha-rewards")
public class AdminGachaRewardController {

    private final GachaRewardService rewardService;

    public AdminGachaRewardController(GachaRewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rewards", rewardService.findAll());
        return "admin/gacha-rewards";
    }

    @PostMapping("/create")
    public String create(@RequestParam String emoji,
                         @RequestParam String name,
                         @RequestParam ECardRarity rarity,
                         @RequestParam(required = false) String description,
                         RedirectAttributes redirectAttributes) {
        GachaReward reward = new GachaReward();
        reward.setEmoji(emoji);
        reward.setName(name);
        reward.setRarity(rarity);
        reward.setDescription(description);
        rewardService.save(reward);
        redirectAttributes.addFlashAttribute("successMessage", "Récompense « " + emoji + " » créée.");
        return "redirect:/admin/gacha-rewards";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String emoji,
                         @RequestParam String name,
                         @RequestParam ECardRarity rarity,
                         @RequestParam(required = false) String description,
                         RedirectAttributes redirectAttributes) {
        rewardService.findById(id).ifPresent(reward -> {
            reward.setEmoji(emoji);
            reward.setName(name);
            reward.setRarity(rarity);
            reward.setDescription(description);
            rewardService.save(reward);
        });
        redirectAttributes.addFlashAttribute("successMessage", "Récompense mise à jour.");
        return "redirect:/admin/gacha-rewards";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        rewardService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Récompense supprimée.");
        return "redirect:/admin/gacha-rewards";
    }
}
