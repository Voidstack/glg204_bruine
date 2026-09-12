package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
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
        String steamId = ((SteamAutenticationToken) authentication).getSteamId();

        Map<String, Object> userAttributes;
        try {
            userAttributes = steamService.getUserData(steamId);
        } catch (Exception e) {
            log.warn("Récupération du profil Steam impossible pour {}", steamId, e);
            return null;
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
        } catch (Exception ignored) {
            // profil privé ou API indisponible les champs restent inchangés
        }

        user = userService.save(user);
        SteamUserPrincipal steamUserPrincipal = SteamUserPrincipal.create(user, userAttributes);

        return new SteamAutenticationToken(steamId, steamUserPrincipal, steamUserPrincipal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(SteamAutenticationToken.class);
    }
}