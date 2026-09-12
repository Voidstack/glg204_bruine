package com.enosistudio.bruine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Encodage des mots de passe des comptes d'administration.
 * <p>
 * Les joueurs n'ont pas de mot de passe chez nous : leur authentification est déléguée à
 * Steam par OpenID. Seule la chaîne admin en a besoin, pour la vérification à la connexion
 * ({@code AdminUserService#loadUserByUsername}) et pour l'encodage à la création de compte.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Encodeur délégant : il encode en BCrypt, et sait relire les autres formats grâce au
     * préfixe stocké devant le hachage (par exemple le {@code {noop}} des comptes de démonstration).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
