package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.security.SteamUserPrincipal;
import com.enosistudio.bruine.steam.service.SteamUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Controller
@RequestMapping("/admin")
public class AdminImpersonationController {

    private final SteamUserService steamUserService;

    public AdminImpersonationController(SteamUserService steamUserService) {
        this.steamUserService = steamUserService;
    }

    @PostMapping("/impersonate/{id}")
    public String impersonate(@PathVariable Long id, HttpSession session) {
        SteamUser user = steamUserService.findById(id).orElse(null);
        if (user == null) return "redirect:/admin";

        SteamUserPrincipal principal = SteamUserPrincipal.create(user);
        SteamAuthenticationToken token = new SteamAuthenticationToken(
                user.getSteamId(), principal, principal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/";
    }
}
