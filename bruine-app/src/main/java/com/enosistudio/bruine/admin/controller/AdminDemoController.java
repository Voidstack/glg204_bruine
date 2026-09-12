package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.security.SteamUserPrincipal;
import com.enosistudio.bruine.steam.security.SteamUserService;
import com.enosistudio.bruine.steam.service.SteamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminDemoController {

    // avatar par default de steam
    private static final String DEFAULT_AVATAR =
            "https://avatars.steamstatic.com/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg";

    private final SteamUserService steamUserService;
    private final SteamService steamService;

    public AdminDemoController(SteamUserService steamUserService, SteamService steamService) {
        this.steamUserService = steamUserService;
        this.steamService = steamService;
    }

    @PostMapping("/impersonate/{id}")
    public String impersonate(@PathVariable Long id, HttpSession session) {
        SteamUser user = steamUserService.findById(id).orElse(null);
        if (user == null) return "redirect:/admin";

        Map<String, Object> attributes;
        try {
            attributes = steamService.getUserData(user.getSteamId());
        } catch (Exception e) {
            attributes = Map.of("personaname", user.getUsername(), "avatar", DEFAULT_AVATAR);
        }

        SteamUserPrincipal principal = SteamUserPrincipal.create(user, attributes);
        SteamAuthenticationToken token = new SteamAuthenticationToken(
                user.getSteamId(), principal, principal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);

        return "redirect:/";
    }
}
