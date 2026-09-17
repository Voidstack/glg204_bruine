package com.enosistudio.bruine.leaderboard.controller;

import com.enosistudio.bruine.leaderboard.dto.LeaderboardEntryDTO;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

import java.util.List;

@Controller
public class LeaderboardController {

    private final SteamUserService steamUserService;

    public LeaderboardController(SteamUserService steamUserService) {
        this.steamUserService = steamUserService;
    }

    @GetMapping("/leaderboard")
    public String leaderboardRoot() {
        return "redirect:/leaderboard/playtime";
    }

    @GetMapping("/leaderboard/{statMode:playtime|xp}")
    public String leaderboard(@PathVariable String statMode, Model model) {
        List<LeaderboardEntryDTO> entries = "xp".equals(statMode)
                ? steamUserService.findLeaderboardByXp().stream().map(LeaderboardEntryDTO::xp).toList()
                : steamUserService.findLeaderboard().stream().map(LeaderboardEntryDTO::playtime).toList();

        model.addAttribute("top3", entries.stream().limit(3).toList());
        model.addAttribute("rest", entries.stream().skip(3).toList());
        return "leaderboard";
    }
}
