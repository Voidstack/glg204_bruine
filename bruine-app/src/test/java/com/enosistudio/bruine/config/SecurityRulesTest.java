package com.enosistudio.bruine.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie que l'autorisation est bien portée par les chaînes de filtres
 * ({@link WebSecurityConfig}) et non par du code recopié dans les contrôleurs.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class SecurityRulesTest {

    @Autowired
    private MockMvc mvc;

    // Pages publiques

    @Test
    void anonyme_accueil_ok() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void anonyme_classement_ok() throws Exception {
        mvc.perform(get("/leaderboard/playtime")).andExpect(status().isOk());
        mvc.perform(get("/leaderboard/xp")).andExpect(status().isOk());
    }

    @Test
    void anonyme_pages_legales_ok() throws Exception {
        mvc.perform(get("/mentions-legales")).andExpect(status().isOk());
        mvc.perform(get("/politique-confidentialite")).andExpect(status().isOk());
    }

    @Test
    void anonyme_images_du_site_ok() throws Exception {
        // les images sont sous /img (et non /images) : elles doivent rester publiques
        mvc.perform(get("/img/bruine_banner_white.png")).andExpect(status().isOk());
        mvc.perform(get("/img/steam_signin.png")).andExpect(status().isOk());
    }

    // Pages joueur : redirection vers la connexion Steam

    @Test
    void anonyme_pagesJoueur_redirigentVersConnexionSteam() throws Exception {
        for (String url : new String[]{"/gacha", "/inventory", "/deck", "/levelup", "/market", "/shop"}) {
            mvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/steam/login"));
        }
    }

    @Test
    void anonyme_apiJson_renvoie401_pasUneRedirection() throws Exception {
        mvc.perform(post("/levelup/convert").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized());
    }

    // Chaîne admin

    @Test
    void anonyme_pagesAdmin_redirigentVersLoginAdmin() throws Exception {
        for (String url : new String[]{"/admin/steam-users", "/admin/gacha-rewards",
                "/admin/shop-packs", "/admin/gacha-config", "/admin/admin-users"}) {
            mvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/admin"));
        }
    }

    @Test
    void anonyme_loginAdmin_estAccessible() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }
}
