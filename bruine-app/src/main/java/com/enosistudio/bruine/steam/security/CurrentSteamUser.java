package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.exception.SteamSessionExpiredException;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Accès au joueur Steam authentifié pour la requête en cours.
 * <p>
 * Ce composant existe pour ne plus recopier dans chaque contrôleur le même bloc de
 * vérification du jeton, qui doit distinguer une session Steam d'une session admin.
 * Un {@link Optional} vide signifie qu'aucun joueur n'est connecté, au contrôleur de
 * décider s'il redirige vers la connexion ou s'il répond une erreur.
 */
@Component
public class CurrentSteamUser {

    private final SteamUserService steamUserService;

    public CurrentSteamUser(SteamUserService steamUserService) {
        this.steamUserService = steamUserService;
    }

    /**
     * Joueur connecté, sans sa collection de cartes.
     */
    public Optional<SteamUser> find() {
        return steamId().flatMap(steamUserService::findBySteamId);
    }

    /**
     * Joueur connecté avec sa collection de cartes déjà chargée.
     * À réserver aux pages qui parcourent la collection, pour éviter une initialisation
     * paresseuse hors transaction.
     */
    public Optional<SteamUser> findWithRewards() {
        return steamId().flatMap(steamUserService::findBySteamIdWithRewards);
    }

    /**
     * Joueur connecté, pour les routes que la chaîne de sécurité réserve déjà à une session Steam.
     * La seule raison d'un échec ici est un compte supprimé alors que la session vit encore
     * (ex. suppression par un admin) : on lève alors {@link SteamSessionExpiredException}.
     */
    public SteamUser require() {
        return find().orElseThrow(SteamSessionExpiredException::new);
    }

    /**
     * Variante de {@link #require()} avec la collection de cartes déjà chargée.
     */
    public SteamUser requireWithRewards() {
        return findWithRewards().orElseThrow(SteamSessionExpiredException::new);
    }

    /**
     * Identifiant Steam du joueur connecté, sans aller en base.
     * <p>
     * C'est le seul endroit qui connaisse la forme du jeton d'authentification : tout le
     * reste de l'application passe par ici plutôt que de relire le {@code SecurityContext}.
     */
    public Optional<String> steamId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof SteamAuthenticationToken token)
                || !token.isAuthenticated()
                || token.getPrincipal() == null) {
            return Optional.empty();
        }
        return Optional.of(token.getPrincipal().steamId());
    }
}
