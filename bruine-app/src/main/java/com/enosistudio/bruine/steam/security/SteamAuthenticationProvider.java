package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Vérifie auprès de Steam l'assertion OpenID du jeton, puis enregistre la connexion du joueur.
 */
@Component
public class SteamAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(SteamAuthenticationProvider.class);

    private final SteamUserService userService;
    private final SteamService steamService;

    public SteamAuthenticationProvider(SteamUserService userService, SteamService steamService) {
        this.userService = userService;
        this.steamService = steamService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String steamId = verifiedSteamId((SteamAuthenticationToken) authentication);

        // Le profil est indispensable : sans lui on ne sait ni nommer le joueur, ni le créer.
        Map<String, Object> userAttributes;
        try {
            userAttributes = steamService.getUserData(steamId);
        } catch (SteamException steamIndisponible) {
            log.warn("Récupération du profil Steam impossible pour {}", steamId, steamIndisponible);
            throw new AuthenticationServiceException(
                    "Profil Steam inaccessible, connexion impossible pour le moment.", steamIndisponible);
        }
        Long totalPlaytime;
        try {
            totalPlaytime = steamService.getTotalPlaytimeMinutes(steamId);
        } catch (SteamException tempsDeJeuIndisponible) {
            // Profil privé ou API muette : les compteurs restent inchangés, la connexion continue.
            totalPlaytime = null;
        }

        // Les appels Steam sont faits avant : le joueur n'est relu et verrouillé qu'au moment
        // d'écrire, pour ne pas écraser un score modifié pendant ces appels réseau.
        SteamUser user = userService.recordLogin(steamId, (String) userAttributes.get("personaname"), totalPlaytime);
        return SteamAuthenticationToken.authenticated(
                SteamUserPrincipal.create(user, (String) userAttributes.get("avatar")));
    }

    /**
     * Identifiant Steam garanti par l'assertion, une fois celle-ci confirmée par Steam.
     */
    private String verifiedSteamId(SteamAuthenticationToken token) {
        try {
            return steamService.validateLoginParameters(token.getCredentials(), token.getBaseUrl());
        } catch (IllegalArgumentException | ConstraintViolationException assertionRefusee) {
            log.warn("Assertion OpenID Steam refusée : {}", assertionRefusee.getMessage());
            throw new BadCredentialsException("Assertion OpenID Steam refusée.", assertionRefusee);
        } catch (RestClientException steamInjoignable) {
            log.warn("Vérification de l'assertion OpenID impossible", steamInjoignable);
            throw new AuthenticationServiceException("Steam injoignable, connexion impossible pour le moment.",
                    steamInjoignable);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SteamAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
