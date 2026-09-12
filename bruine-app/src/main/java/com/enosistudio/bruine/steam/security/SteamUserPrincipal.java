package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.model.SteamUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Identité du joueur Steam connecté, telle que Spring Security la conserve en session.
 * <p>
 * Elle ne porte que ce qui identifie, autorise, et s'affiche dans la barre de navigation.
 * Le score en est volontairement absent : un instantané pris à la connexion serait faux dès
 * le premier tirage, il est relu à chaque rendu par {@code SteamControllerAdvice}. Le pseudo
 * et l'avatar, eux, ne bougent qu'entre deux connexions.
 */
public record SteamUserPrincipal(String steamId, String username, String avatarUrl,
                                 Collection<? extends GrantedAuthority> authorities) implements UserDetails {

    /**
     * Tous les joueurs ont le même rôle : la liste est partagée plutôt que recréée à chaque connexion.
     */
    private static final List<GrantedAuthority> JOUEUR = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    /**
     * Avatar générique de Steam, servi quand le profil n'en fournit pas.
     */
    private static final String AVATAR_PAR_DEFAUT =
            "https://avatars.steamstatic.com/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg";

    /**
     * @param avatarUrl vignette du profil Steam, ou {@code null} pour l'avatar générique
     */
    public static SteamUserPrincipal create(SteamUser user, String avatarUrl) {
        String avatar = (avatarUrl == null || avatarUrl.isBlank()) ? AVATAR_PAR_DEFAUT : avatarUrl;
        return new SteamUserPrincipal(user.getSteamId(), user.getUsername(), avatar, JOUEUR);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Aucun mot de passe ici : l'authentification est déléguée à Steam par OpenID.
     */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
