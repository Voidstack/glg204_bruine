package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.admin.EAdminRole;
import com.enosistudio.bruine.admin.mfa.AdminSecurityContextService;
import com.enosistudio.bruine.admin.mfa.TotpService;
import com.enosistudio.bruine.admin.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Contrôleur MFA (TOTP) pour les comptes admin.
 * /admin/mfa - 2e étape de connexion : saisie du code (état {@code ROLE_PRE_MFA})
 * /admin/mfa/setup - enrôlement : QR code + confirmation (admin déjà connecté)
 * /admin/mfa/disable - désactivation de sa propre MFA
 *
 * <p>Les rôles requis sont posés par la chaîne de sécurité admin :
 * {@code /admin/mfa} accepte {@code ROLE_ADMIN} ou {@code ROLE_PRE_MFA},
 * {@code /admin/mfa/**} exige {@code ROLE_ADMIN}.
 */
@Controller
@RequestMapping("/admin/mfa")
public class AdminMfaController {

    private static final String SETUP_SECRET_ATTR = "MFA_SETUP_SECRET";

    private final AdminUserService adminUserService;
    private final TotpService totpService;
    private final AdminSecurityContextService adminSecurityContext;

    public AdminMfaController(AdminUserService adminUserService,
                              TotpService totpService,
                              AdminSecurityContextService adminSecurityContext) {
        this.adminUserService = adminUserService;
        this.totpService = totpService;
        this.adminSecurityContext = adminSecurityContext;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }

    // 2e étape de connexion

    @GetMapping
    public String verifyPage(@RequestParam(required = false, name = "error") String error,
                             Authentication authentication,
                             Model model) {
        // déjà admin complet : le 2e facteur est inutile
        if (hasRole(authentication, EAdminRole.ADMIN.authority())) {
            return "redirect:/admin";
        }
        model.addAttribute("mfaError", error != null);
        return "admin/mfa-verify";
    }

    @PostMapping
    public String verify(@RequestParam String code,
                         Authentication authentication,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        if (!hasRole(authentication, EAdminRole.PRE_MFA.authority())) {
            return "redirect:/admin";
        }
        String username = authentication.getName();
        String secret = adminUserService.getMfaSecret(username);

        if (!totpService.verify(secret, code)) {
            return "redirect:/admin/mfa?error";
        }
        Authentication full = UsernamePasswordAuthenticationToken.authenticated(
                username, null, List.of(new SimpleGrantedAuthority(EAdminRole.ADMIN.authority())));
        adminSecurityContext.save(full, request, response);
        return "redirect:/admin";
    }

    @GetMapping("/setup")
    public String setupPage(Authentication authentication, HttpSession session, Model model) {
        String username = authentication.getName();
        boolean enabled = adminUserService.isMfaEnabled(username);
        model.addAttribute("mfaEnabled", enabled);

        if (!enabled) {
            String secret = totpService.generateSecret();
            session.setAttribute(SETUP_SECRET_ATTR, secret);
            model.addAttribute("secret", secret);
            model.addAttribute("qrCode", totpService.qrCodeDataUri(username, secret));
        }
        return "admin/mfa-setup";
    }

    @PostMapping("/setup")
    public String confirmSetup(@RequestParam String code,
                               Authentication authentication,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String secret = (String) session.getAttribute(SETUP_SECRET_ATTR);
        if (totpService.verify(secret, code)) {
            adminUserService.enableMfa(authentication.getName(), secret);
            session.removeAttribute(SETUP_SECRET_ATTR);
            redirectAttributes.addFlashAttribute("successMessage", "MFA activée avec succès.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Code invalide. Réessayez en scannant à nouveau le QR code.");
        }
        return "redirect:/admin/mfa/setup";
    }

    @PostMapping("/disable")
    public String disableOwn(Authentication authentication, RedirectAttributes redirectAttributes) {
        adminUserService.disableMfa(authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "MFA désactivée.");
        return "redirect:/admin/mfa/setup";
    }
}
