package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.dto.SteamOpenidLoginDTO;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Authentification Steam, sur le modèle de {@code UsernamePasswordAuthenticationToken} :
 * non authentifiée, elle porte l'assertion OpenID renvoyée par Steam ; authentifiée, le joueur.
 */
public class SteamAuthenticationToken extends AbstractAuthenticationToken {

    private final SteamUserPrincipal principal;
    private final SteamOpenidLoginDTO assertion;

    /**
     * Adresse publique du site attendue dans le {@code return_to} de l'assertion.
     */
    @Getter
    private final String baseUrl;

    private SteamAuthenticationToken(SteamOpenidLoginDTO assertion, String baseUrl) {
        super(null);
        this.principal = null;
        this.assertion = assertion;
        this.baseUrl = baseUrl;
        setAuthenticated(false);
    }

    private SteamAuthenticationToken(SteamUserPrincipal principal) {
        super(principal.getAuthorities());
        this.principal = principal;
        this.assertion = null;
        this.baseUrl = null;
        setAuthenticated(true);
    }

    /**
     * Demande d'authentification construite par {@link SteamOpenIdAuthenticationFilter}.
     */
    public static SteamAuthenticationToken unauthenticated(SteamOpenidLoginDTO assertion, String baseUrl) {
        return new SteamAuthenticationToken(assertion, baseUrl);
    }

    /**
     * Joueur authentifié, produit par {@link SteamAuthenticationProvider}.
     */
    public static SteamAuthenticationToken authenticated(SteamUserPrincipal principal) {
        return new SteamAuthenticationToken(principal);
    }

    @Override
    public SteamOpenidLoginDTO getCredentials() {
        return assertion;
    }

    @Override
    public SteamUserPrincipal getPrincipal() {
        return principal;
    }
}
