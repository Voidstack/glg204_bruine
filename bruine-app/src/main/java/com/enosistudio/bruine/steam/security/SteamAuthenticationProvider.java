package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.exception.SteamException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamService;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        String steamId = ((SteamAuthenticationToken) authentication).getSteamId();

        // Le profil est indispensable : sans lui on ne sait ni nommer le joueur, ni le créer.
        // On lève plutôt que de retourner null, qui signifierait « ce provider ne sait pas
        // traiter ce jeton » et ferait remonter une ProviderNotFoundException trompeuse.
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
            // Contrairement au profil ci-dessus, le temps de jeu n'est pas indispensable au login.
            totalPlaytime = null;
        }

        // Les appels Steam sont faits avant : le joueur n'est relu et verrouillé qu'au moment
        // d'écrire, pour ne pas écraser un score modifié pendant ces appels réseau.
        SteamUser user = userService.recordLogin(steamId, (String) userAttributes.get("personaname"), totalPlaytime);
        SteamUserPrincipal steamUserPrincipal = SteamUserPrincipal.create(user, (String) userAttributes.get("avatar"));

        return new SteamAuthenticationToken(steamId, steamUserPrincipal, steamUserPrincipal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(SteamAuthenticationToken.class);
    }
}