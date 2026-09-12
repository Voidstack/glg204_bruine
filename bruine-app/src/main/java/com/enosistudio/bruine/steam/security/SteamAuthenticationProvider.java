package com.enosistudio.bruine.steam.security;

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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
        } catch (Exception steamIndisponible) {
            log.warn("Récupération du profil Steam impossible pour {}", steamId, steamIndisponible);
            throw new AuthenticationServiceException(
                    "Profil Steam inaccessible, connexion impossible pour le moment.", steamIndisponible);
        }
        Optional<SteamUser> userOptional = userService.findBySteamId(steamId);
        SteamUser user = userOptional.orElseGet(() -> {
            String username = (String) userAttributes.get("personaname");
            return new SteamUser(null, steamId, username);
        });
        user.setLastLoginAt(LocalDateTime.now());

        try {
            long totalPlaytime = steamService.getTotalPlaytimeMinutes(steamId);
            if (user.getInitialPlaytimeMinutes() == null) {
                user.setInitialPlaytimeMinutes(totalPlaytime);
            }
            user.setCurrentPlaytimeMinutes(totalPlaytime);
        } catch (Exception tempsDeJeuIndisponible) {
            // Profil privé ou API muette : les compteurs restent inchangés, la connexion continue.
            // Contrairement au profil ci-dessus, le temps de jeu n'est pas indispensable au login.
        }

        user = userService.save(user);
        SteamUserPrincipal steamUserPrincipal = SteamUserPrincipal.create(user);

        return new SteamAuthenticationToken(steamId, steamUserPrincipal, steamUserPrincipal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(SteamAuthenticationToken.class);
    }
}