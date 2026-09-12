package com.enosistudio.bruine.admin.mfa;

import com.enosistudio.bruine.admin.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Handler exécuté après validation du mot de passe admin.
 *
 * <p>Si le compte a la MFA activée, l'authentification complète (ROLE_ADMIN) déjà persistée par le
 * filtre est <b>rétrogradée</b> en {@code ROLE_PRE_MFA} : l'admin n'a alors accès qu'à la page de
 * vérification {@code /admin/mfa}, tant qu'il n'a pas saisi son code TOTP. Sinon, connexion normale.
 */
@Component
public class MfaAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String ROLE_PRE_MFA = "ROLE_PRE_MFA";

    private final AdminUserService adminUserService;
    private final AdminSecurityContextService adminSecurityContext;

    public MfaAuthenticationSuccessHandler(AdminUserService adminUserService,
                                           AdminSecurityContextService adminSecurityContext) {
        this.adminUserService = adminUserService;
        this.adminSecurityContext = adminSecurityContext;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String username = authentication.getName();

        if (adminUserService.isMfaEnabled(username)) {
            Authentication preMfa = UsernamePasswordAuthenticationToken.authenticated(
                    username, null, List.of(new SimpleGrantedAuthority(ROLE_PRE_MFA)));
            adminSecurityContext.save(preMfa, request, response);
            response.sendRedirect(request.getContextPath() + "/admin/mfa");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin");
        }
    }
}
