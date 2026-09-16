package com.enosistudio.bruine.steam.exception;

/**
 * Steam n'a pas rendu la donnée attendue : panne, erreur HTTP, JSON illisible ou profil privé.
 * Non vérifiée comme le reste du projet ; les appelants qui savent se rabattre sur autre chose
 * l'attrapent, les autres laissent remonter.
 */
public class SteamException extends RuntimeException {

    public SteamException(String message) {
        super(message);
    }

    public SteamException(String message, Throwable cause) {
        super(message, cause);
    }
}
