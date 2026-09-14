package com.enosistudio.bruine.config;

import com.enosistudio.bruine.admin.mfa.AdminSecurityContextService;
import com.enosistudio.bruine.admin.mfa.EAdminRole;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        when(steamService.getUserData("76561198100881386")).thenReturn(Map.of("personaname", "Joueur"));
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
                .thenThrow(new IllegalArgumentException("Assertion OpenID destinée à une autre adresse"));

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
    void anonymousJsonRequestGetsUnauthorizedInsteadOfARedirect() throws Exception {
        mvc.perform(post("/levelup/convert").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized());
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
    void anonymousCanOpenTheAdminLoginPage() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }
}
