package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.dto.SteamOpenidLoginDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Retour de Steam après connexion : transforme les paramètres {@code openid.*} en
 * {@link SteamAuthenticationToken} et le soumet au {@link SteamAuthenticationProvider}.
 */
public class SteamOpenIdAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public SteamOpenIdAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/steam/login/redirect"),
                authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        SteamOpenidLoginDTO assertion = new SteamOpenidLoginDTO(
                request.getParameter("openid.ns"),
                request.getParameter("openid.op_endpoint"),
                request.getParameter("openid.claimed_id"),
                request.getParameter("openid.identity"),
                request.getParameter("openid.return_to"),
                request.getParameter("openid.response_nonce"),
                request.getParameter("openid.assoc_handle"),
                request.getParameter("openid.signed"),
                request.getParameter("openid.sig"));
        return getAuthenticationManager().authenticate(
                SteamAuthenticationToken.unauthenticated(assertion, baseUrl(request)));
    }

    /**
     * Base publique du site, calculée de la même façon à l'aller et au retour de Steam
     * pour que le {@code return_to} de l'assertion puisse être comparé.
     */
    public static String baseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .toUriString();
    }
}
