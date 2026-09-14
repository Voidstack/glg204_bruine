package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/steam-users")
public class AdminSteamUserController {

    private final SteamUserService steamUserService;
    private final SteamService steamService;

    public AdminSteamUserController(SteamUserService steamUserService, SteamService steamService) {
        this.steamUserService = steamUserService;
        this.steamService = steamService;
    }

    @GetMapping
    public String list(Model model) {
        List<SteamUser> steamUsers = steamUserService.findAll();
        model.addAttribute("steamUsers", steamUsers);
        model.addAttribute("onlineSteamIds", onlineSteamIds(steamUsers));
        return "admin/steam-users";
    }

    private Set<String> onlineSteamIds(List<SteamUser> steamUsers) {
        return steamUsers.stream()
                .map(SteamUser::getSteamId)
                .filter(this::isOnline)
                .collect(Collectors.toSet());
    }

    /**
     * Hors ligne si Steam ne répond pas, la page reste utilisable.
     */
    private boolean isOnline(String steamId) {
        try {
            return steamService.isOnline(steamId);
        } catch (SteamException steamIndisponible) {
            return false;
        }
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
