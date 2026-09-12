package com.enosistudio.bruine.admin.mfa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Encapsule le dépôt de contexte de sécurité admin (clé de session {@code ADMIN_SECURITY_CONTEXT}).
 *
 * <p>On expose un type dédié (et non un bean {@code SecurityContextRepository}) pour être partagé
 * entre la configuration de la chaîne admin, le success handler MFA et le contrôleur MFA,
 * <b>sans risquer</b> qu'un bean {@code SecurityContextRepository} global soit appliqué par erreur
 * à la chaîne principale (ce qui casserait l'isolation des sessions Steam / admin).
 */
@Service
public class AdminSecurityContextService {

    public static final String CONTEXT_KEY = "ADMIN_SECURITY_CONTEXT";

    private final HttpSessionSecurityContextRepository repository;

    public AdminSecurityContextService() {
        this.repository = new HttpSessionSecurityContextRepository();
        this.repository.setSpringSecurityContextKey(CONTEXT_KEY);
    }

    /**
     * Dépôt à brancher sur la chaîne admin via {@code securityContext(...)}.
     */
    public HttpSessionSecurityContextRepository repository() {
        return repository;
    }

    /**
     * Remplace l'authentification admin courante et la persiste dans la session admin.
     * Utilisé pour le downgrade (pré-MFA) et l'upgrade (après vérification du code).
     */
    public void save(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }
}
