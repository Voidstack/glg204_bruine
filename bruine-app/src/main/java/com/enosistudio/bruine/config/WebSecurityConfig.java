package com.enosistudio.bruine.config;

import com.enosistudio.bruine.admin.mfa.AdminSecurityContextService;
import com.enosistudio.bruine.admin.mfa.EAdminRole;
import com.enosistudio.bruine.admin.mfa.MfaAuthenticationSuccessHandler;
import com.enosistudio.bruine.steam.security.SteamAuthenticationProvider;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
public class WebSecurityConfig {

    private final SteamAuthenticationProvider steamAuthenticationProvider;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public WebSecurityConfig(SteamAuthenticationProvider steamAuthenticationProvider, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.steamAuthenticationProvider = steamAuthenticationProvider;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Chaîne admin, contexte de session isolé du site principal.
     * L'authentification est stockée sous la clé {@link AdminSecurityContextService#CONTEXT_KEY},
     * séparée de la clé par défaut utilisée par la chaîne principale.
     * Un admin connecté ici apparaît donc comme anonyme sur le reste du site.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http, AdminSecurityContextService adminSecurityContext, MfaAuthenticationSuccessHandler mfaSuccessHandler) throws Exception {
        // @formatter:off
        http
                .securityMatcher("/admin", "/admin/**")
                .securityContext(sc -> sc.securityContextRepository(adminSecurityContext.repository()))
                .authorizeHttpRequests(a -> a
                        // page de login
                        .requestMatchers("/admin").permitAll()
                        // 2e facteur : accessible tant qu'on n'a pas fini le MFA
                        .requestMatchers("/admin/mfa").hasAnyAuthority(EAdminRole.ADMIN.authority(), EAdminRole.PRE_MFA.authority())
                        // tout le reste de /admin/** exige un admin complet
                        .anyRequest().hasAuthority(EAdminRole.ADMIN.authority())
                )
                // non connecté -> page de login ; connecté sans le bon rôle (ex. PRE_MFA) -> /admin
                .exceptionHandling(e -> e.accessDeniedHandler(
                        (request, response, ex) -> response.sendRedirect(request.getContextPath() + "/admin")))
                .formLogin(l -> l
                        .loginPage("/admin")
                        .loginProcessingUrl("/admin")
                        .successHandler(mfaSuccessHandler)
                        .failureUrl("/admin?error"))
                .authenticationProvider(daoAuthenticationProvider())
        ;
        // @formatter:on

        return http.build();
    }

    /**
     * Chaîne principale, site public et authentification Steam.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain mainFilterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http.authorizeHttpRequests(a -> a
                        // ressources statiques (/css, /js, /images, /webjars, favicon) et page d'erreur
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        // les images du site sont sous /img (hors emplacements « communs » de Spring)
                        .requestMatchers(HttpMethod.GET, "/img/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // pages publiques
                        .requestMatchers(HttpMethod.GET, "/",
                                "/leaderboard", "/leaderboard/**",
                                "/mentions-legales", "/politique-confidentialite").permitAll()
                        // authentification Steam
                        .requestMatchers("/steam/login", "/steam/login/redirect", "/steam/failed").permitAll()
                        // profils Steam publics — mais pas la suppression de compte
                        .requestMatchers(HttpMethod.GET, "/steam/profile", "/steam/profile/*", "/steam/profile/*/games").permitAll()
                        // image SVG du deck d'un joueur, partageable via <img>
                        .requestMatchers(HttpMethod.GET, "/deck/7656119*").permitAll()
                        // tout le reste (gacha, inventaire, deck, market, shop, levelup, /steam/logout, delete...) exige une session Steam
                        .anyRequest().authenticated()
                )
                .anonymous(Customizer.withDefaults())
                // requête API (Accept: application/json) -> 401 ; navigation navigateur -> page de connexion Steam
                .exceptionHandling(e -> {
                    MediaTypeRequestMatcher jsonRequest = new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON);
                    jsonRequest.setUseEquals(true);
                    e.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), jsonRequest);
                    e.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/steam/login"), AnyRequestMatcher.INSTANCE);
                })
                // on ne fait pas  .logout(l -> l.logoutSuccessUrl("/")) pour éviter de déco l'admin en même temps que le user
                .logout(AbstractHttpConfigurer::disable)
                .authenticationProvider(steamAuthenticationProvider)
                .sessionManagement(sm ->
                        sm.sessionConcurrency(sc ->
                                sc.maximumSessions(-1)
                                .sessionRegistry(sessionRegistry())
                        )
                )
        ;
        // @formatter:on

        return http.build();
    }

    private DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
