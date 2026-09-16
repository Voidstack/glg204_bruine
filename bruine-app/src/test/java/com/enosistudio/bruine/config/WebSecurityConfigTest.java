package com.enosistudio.bruine.config;

import com.enosistudio.bruine.admin.EAdminRole;
import com.enosistudio.bruine.admin.mfa.AdminSecurityContextService;
import com.enosistudio.bruine.steam.dto.SteamGameDTO;
import com.enosistudio.bruine.steam.dto.SteamPlayerDTO;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.SteamAuthenticationToken;
import com.enosistudio.bruine.steam.security.SteamUserPrincipal;
import com.enosistudio.bruine.steam.service.SteamService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SteamService steamService;

    @Test
    void aValidSteamAssertionSignsThePlayerInWithAFreshSession() throws Exception {
        when(steamService.validateLoginParameters(any(), any())).thenReturn("76561198100881386");
        when(steamService.getPlayer("76561198100881386")).thenReturn(
                new SteamPlayerDTO("76561198100881386", "Joueur", null, null, null, 0, 0, null, null));
        MockHttpSession session = new MockHttpSession();
        String idBeforeLogin = session.getId();

        MvcResult result = mvc.perform(get("/steam/login/redirect").session(session))
                .andExpect(redirectedUrl("/"))
                .andReturn();

        HttpSession after = result.getRequest().getSession(false);
        SecurityContext context = (SecurityContext) after.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
        assertInstanceOf(SteamAuthenticationToken.class, context.getAuthentication());
        assertNotEquals(idBeforeLogin, after.getId());
    }

    @Test
    void aSteamAssertionRefusedLeadsToTheLoginFailurePage() throws Exception {
        when(steamService.validateLoginParameters(any(), any()))
                .thenThrow(new BadCredentialsException("Assertion OpenID destinée à une autre adresse"));

        mvc.perform(get("/steam/login/redirect"))
                .andExpect(redirectedUrl("/steam/failed"));
    }

    @Test
    void steamLogoutKeepsTheAdminSignedIn() throws Exception {
        MockHttpSession session = sessionWithSteamAndAdminSignedIn();

        mvc.perform(post("/steam/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/"));

        assertNull(session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
        assertNotNull(session.getAttribute(AdminSecurityContextService.CONTEXT_KEY));
    }

    @Test
    void adminLogoutKeepsTheSteamPlayerSignedIn() throws Exception {
        MockHttpSession session = sessionWithSteamAndAdminSignedIn();

        mvc.perform(post("/admin/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/admin"));

        assertNull(session.getAttribute(AdminSecurityContextService.CONTEXT_KEY));
        assertNotNull(session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void anAdminWaitingForTheirMfaCodeCanLogOut() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminSecurityContextService.CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("admin", null,
                        List.of(new SimpleGrantedAuthority(EAdminRole.PRE_MFA.authority())))));

        mvc.perform(post("/admin/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/admin"));

        assertNull(session.getAttribute(AdminSecurityContextService.CONTEXT_KEY));
    }

    private MockHttpSession sessionWithSteamAndAdminSignedIn() {
        SteamUserPrincipal player = SteamUserPrincipal.create(
                new SteamUser(1L, "76561198100881386", "Joueur"), null);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(SteamAuthenticationToken.authenticated(player)));
        session.setAttribute(AdminSecurityContextService.CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("admin", null,
                        List.of(new SimpleGrantedAuthority(EAdminRole.ADMIN.authority())))));
        return session;
    }

    @Test
    void anonymousCanOpenTheHomePage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void anonymousCanOpenTheLeaderboards() throws Exception {
        mvc.perform(get("/leaderboard/playtime")).andExpect(status().isOk());
        mvc.perform(get("/leaderboard/xp")).andExpect(status().isOk());
    }

    @Test
    void anonymousCanOpenTheLegalPages() throws Exception {
        mvc.perform(get("/mentions-legales")).andExpect(status().isOk());
        mvc.perform(get("/politique-confidentialite")).andExpect(status().isOk());
    }

    @Test
    void anonymousCanLoadTheSiteImages() throws Exception {
        mvc.perform(get("/img/bruine_banner_white.png")).andExpect(status().isOk());
        mvc.perform(get("/img/steam_signin.png")).andExpect(status().isOk());
    }

    @Test
    void anonymousIsRedirectedToSteamLoginFromPlayerPages() throws Exception {
        for (String url : new String[]{"/gacha", "/inventory", "/deck", "/levelup", "/market", "/shop"}) {
            mvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/steam/login"));
        }
    }

    @Test
    void anonymousIsRedirectedToAdminLoginFromAdminPages() throws Exception {
        for (String url : new String[]{"/admin/steam-users", "/admin/gacha-rewards",
                "/admin/shop-packs", "/admin/gacha-config", "/admin/admin-users"}) {
            mvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/admin"));
        }
    }

    @Test
    void stripeReachesTheWebhookWithoutSessionNorCsrfToken() throws Exception {
        mvc.perform(post("/shop/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=invalide")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCanOpenASteamProfileAndItsGames() throws Exception {
        when(steamService.getPlayer("76561198100881386")).thenReturn(
                new SteamPlayerDTO("76561198100881386", "Joueur", null, null, null, 1, 3, "FR", null));
        when(steamService.getPlayedGames("76561198100881386"))
                .thenReturn(List.of(new SteamGameDTO(620, "Portal 2", 90, "abc")));

        mvc.perform(get("/steam/profile/76561198100881386")).andExpect(status().isOk());
        mvc.perform(get("/steam/profile/76561198100881386/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appId").value(620))
                .andExpect(jsonPath("$[0].playtimeMinutes").value(90))
                .andExpect(jsonPath("$[0].iconHash").value("abc"));
    }

    @Test
    void aMalformedSteamIdLeadsToNoPage() throws Exception {
        MockHttpSession session = sessionWithSteamAndAdminSignedIn();

        for (String url : new String[]{"/steam/profile/123", "/steam/profile/123/games", "/deck/123"}) {
            mvc.perform(get(url).session(session)).andExpect(status().isNotFound());
        }
    }

    @Test
    void anonymousCanOpenTheAdminLoginPage() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }
}
