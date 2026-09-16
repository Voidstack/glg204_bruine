package com.enosistudio.bruine.leaderboard.controller;

import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

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

    @GetMapping("/leaderboard/playtime")
    public ModelAndView leaderboardPlaytime() {
        List<SteamUser> all = steamUserService.findLeaderboard();
        ModelAndView mav = new ModelAndView("leaderboard");
        mav.addObject("top3", all.stream().limit(3).toList());
        mav.addObject("rest", all.stream().skip(3).toList());
        mav.addObject("statMode", "playtime");
        return mav;
    }

    @GetMapping("/leaderboard/xp")
    public ModelAndView leaderboardXp() {
        List<SteamUser> all = steamUserService.findLeaderboardByXp();
        ModelAndView mav = new ModelAndView("leaderboard");
        mav.addObject("top3", all.stream().limit(3).toList());
        mav.addObject("rest", all.stream().skip(3).toList());
        mav.addObject("statMode", "xp");
        return mav;
    }
}