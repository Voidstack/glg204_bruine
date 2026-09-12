package com.enosistudio.bruine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration de l'authentification.
 * 
 * Deux approches possibles :
 * 
 * <ul>
 * <li> définir un bean UserDetailsService (pas mal d'outils simples pour ça)
 * <li> ou  définir un AuthenticationManager à la main.
 * </ul>
 * 
 * Nous choisissons la seconde approche, pour minimiser les modifications à 
 * ce projet. Ceci dit, la confusion entre la table métier "auteur" et 
 * la notion d'utilisateur est une maladresse de notre part.
 * 
 * Même si un utilisateur correspond à un auteur potentiel, ce sont deux notions
 * séparées. Si par exemple on externalise le login, la table auteur sera modifiée.
 * 
 * Voir https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
 */
@Configuration
public class AuthentificationConfig {
   
	/**
	 * On met en place cet encoder pour pouvoir l'utiliser quand on veut sauver des objets utilisateurs.
     * <p> Cet encodeur utilise BCrypt en codage, et admet de nombreux systèmes de codage en validation.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
