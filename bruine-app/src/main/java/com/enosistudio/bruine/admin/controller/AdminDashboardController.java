package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.admin.mfa.AdminSecurityContextService;
import com.enosistudio.bruine.admin.mfa.EAdminRole;
import com.enosistudio.bruine.admin.service.AdminUserService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminDashboardController {

    private final SteamUserService steamUserService;
    private final AdminUserService adminUserService;

    public AdminDashboardController(SteamUserService steamUserService, AdminUserService adminUserService) {
        this.steamUserService = steamUserService;
        this.adminUserService = adminUserService;
    }

    @GetMapping("/admin")
    public String admin(@RequestParam(required = false, name = "error") String error,
                        Authentication authentication,
                        Model model) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority(EAdminRole.ADMIN.authority()));
        if (isAdmin) {
            model.addAttribute("steamUsers", steamUserService.findAll());
            model.addAttribute("mfaEnabled", adminUserService.isMfaEnabled(authentication.getName()));
            return "admin/dashboard";
        }
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        return "admin/login";
    }

    @PostMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.removeAttribute(AdminSecurityContextService.CONTEXT_KEY);
        return "redirect:/admin";
    }
}