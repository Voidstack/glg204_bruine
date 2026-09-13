package com.enosistudio.bruine.steam.exception;

/**
 * La session Steam est authentifiée mais le compte associé n'existe plus en base
 * (typiquement supprimé par un admin pendant que le joueur navigait).
 * Traité par {@code SteamControllerAdvice} : on nettoie la session et on renvoie à l'accueil.
 */
public class SteamSessionExpiredException extends RuntimeException {
}
