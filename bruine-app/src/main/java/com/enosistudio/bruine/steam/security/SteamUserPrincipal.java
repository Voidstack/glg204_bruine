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
 * Elle ne porte que ce qui identifie et autorise. Tout ce qui bouge en cours de partie
 * (score, profil Steam) en est volontairement absent : un instantané pris à la connexion
 * serait faux dès le premier tirage, et ces données sont relues à la demande.
 */
public record SteamUserPrincipal(String steamId, String username,
                                 Collection<? extends GrantedAuthority> authorities) implements UserDetails {

    /**
     * Tous les joueurs ont le même rôle : la liste est partagée plutôt que recréée à chaque connexion.
     */
    private static final List<GrantedAuthority> JOUEUR = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    public static SteamUserPrincipal create(SteamUser user) {
        return new SteamUserPrincipal(user.getSteamId(), user.getUsername(), JOUEUR);
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
