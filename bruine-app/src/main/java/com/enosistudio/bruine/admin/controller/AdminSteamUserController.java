package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.steam.security.SteamUserPrincipal;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/steam-users")
public class AdminSteamUserController {

    private final SteamUserService steamUserService;
    private final SessionRegistry sessionRegistry;

    public AdminSteamUserController(SteamUserService steamUserService, SessionRegistry sessionRegistry) {
        this.steamUserService = steamUserService;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("steamUsers", steamUserService.findAll());
        Set<String> activeSteamIds = sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof SteamUserPrincipal)
                .filter(p -> !sessionRegistry.getAllSessions(p, false).isEmpty())
                .map(p -> ((SteamUserPrincipal) p).steamId())
                .collect(Collectors.toSet());
        model.addAttribute("activeSteamIds", activeSteamIds);
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
