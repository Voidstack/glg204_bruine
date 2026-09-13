package com.enosistudio.bruine.admin.mfa;

/**
 * Rôles de la chaîne d'administration.
 * <p>
 * {@link #PRE_MFA} est l'état intermédiaire : mot de passe validé, second facteur pas encore.
 * Seul {@code /admin/mfa} l'accepte, tout le reste exige {@link #ADMIN}.
 * <p>
 * Les valeurs portent le préfixe {@code ROLE_} qu'attend {@code SimpleGrantedAuthority}.
 * C'est aussi pourquoi {@code WebSecurityConfig} les consomme avec {@code hasAuthority(...)}
 * et non {@code hasRole(...)}, qui ajouterait un second préfixe : les deux seules chaines
 * de rôle du projet sont ainsi écrites ici, et nulle part ailleurs.
 */
public final class AdminRoles {

    public static final String ADMIN = "ROLE_ADMIN";
    public static final String PRE_MFA = "ROLE_PRE_MFA";

    private AdminRoles() {
    }
}
