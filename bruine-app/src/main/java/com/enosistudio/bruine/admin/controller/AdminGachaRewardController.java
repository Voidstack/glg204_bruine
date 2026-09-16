package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.gacha.dto.GachaRewardFormDTO;
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
    public String create(@ModelAttribute GachaRewardFormDTO form, RedirectAttributes redirectAttributes) {
        rewardService.create(form);
        redirectAttributes.addFlashAttribute("successMessage", "Récompense « " + form.emoji() + " » créée.");
        return "redirect:/admin/gacha-rewards";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute GachaRewardFormDTO form,
                         RedirectAttributes redirectAttributes) {
        rewardService.update(id, form);
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
