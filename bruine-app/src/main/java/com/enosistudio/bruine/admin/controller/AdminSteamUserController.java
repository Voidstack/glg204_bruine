package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/steam-users")
public class AdminSteamUserController {

    private final SteamUserService steamUserService;

    public AdminSteamUserController(SteamUserService steamUserService) {
        this.steamUserService = steamUserService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("steamUsers", steamUserService.findAll());
        return "admin/steam-users";
    }

    @PostMapping("/{id}/score")
    public String updateScore(@PathVariable Long id,
                              @RequestParam int score,
                              RedirectAttributes redirectAttributes) {
        steamUserService.updateScore(id, score);
        redirectAttributes.addFlashAttribute("success", "Score mis à jour.");
        return "redirect:/admin/steam-users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        steamUserService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Compte supprimé.");
        return "redirect:/admin/steam-users";
    }
}
