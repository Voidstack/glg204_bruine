package com.enosistudio.bruine.steam.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SteamAuthenticationToken extends AbstractAuthenticationToken {

    private final SteamUserPrincipal principal;
    @Getter
    private final String steamId;

    public SteamAuthenticationToken(SteamUserPrincipal principal) {
        super(null);
        this.principal = principal;
        this.steamId = null;
        this.setAuthenticated(false);
    }

    public SteamAuthenticationToken(String steamId) {
        super(null);
        this.steamId = steamId;
        this.principal = null;
        this.setAuthenticated(false);
    }

    public SteamAuthenticationToken(String steamId, SteamUserPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.steamId = steamId;
        this.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public SteamUserPrincipal getPrincipal() {
        return this.principal;
    }
}