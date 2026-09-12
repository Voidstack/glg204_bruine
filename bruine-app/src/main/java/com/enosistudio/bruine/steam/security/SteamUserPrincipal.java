package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.model.SteamUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record SteamUserPrincipal(Long id, String steamId, String username, int score, Map<String, Object> attributes, Collection<? extends GrantedAuthority> authorities) implements UserDetails {
    public static SteamUserPrincipal create(SteamUser user, Map<String, Object> attributes) {
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

        return new SteamUserPrincipal(user.getId(), user.getSteamId(), user.getUsername(), user.getScore(), Collections.unmodifiableMap(attributes), authorities);
    }

    public long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.username;
    }
}