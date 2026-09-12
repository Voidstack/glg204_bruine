package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.admin.exception.UsernameAlreadyExistsException;
import com.enosistudio.bruine.admin.service.AdminUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/admin-users")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/admin-users";
    }

    @PostMapping("/create")
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(username, password);
            redirectAttributes.addFlashAttribute("success", "Compte « " + username + " » créé.");
        } catch (UsernameAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("error", "Le nom d'utilisateur « " + username + " » existe déjà.");
        }
        return "redirect:/admin/admin-users";
    }

    @PostMapping("/{username}/delete")
    public String delete(@PathVariable String username,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (username.equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer votre propre compte.");
            return "redirect:/admin/admin-users";
        }
        userService.deleteUser(username);
        redirectAttributes.addFlashAttribute("success", "Compte « " + username + " » supprimé.");
        return "redirect:/admin/admin-users";
    }

    @PostMapping("/{username}/password")
    public String updatePassword(@PathVariable String username,
                                 @RequestParam String newPassword,
                                 RedirectAttributes redirectAttributes) {
        userService.updatePassword(username, newPassword);
        redirectAttributes.addFlashAttribute("success", "Mot de passe de « " + username + " » mis à jour.");
        return "redirect:/admin/admin-users";
    }

    @PostMapping("/{username}/mfa/disable")
    public String disableMfa(@PathVariable String username,
                             RedirectAttributes redirectAttributes) {
        userService.disableMfa(username);
        redirectAttributes.addFlashAttribute("success", "MFA de « " + username + " » désactivée.");
        return "redirect:/admin/admin-users";
    }
}
