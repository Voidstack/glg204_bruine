package com.enosistudio.bruine.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mvc;

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
