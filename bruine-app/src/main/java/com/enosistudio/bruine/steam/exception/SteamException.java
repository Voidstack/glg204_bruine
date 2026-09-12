package com.enosistudio.bruine.steam.exception;

/**
 * L'API Steam n'a pas répondu ce qu'on attendait : code d'erreur HTTP, ou profil introuvable.
 * Le message dit laquelle des deux, il n'est destiné qu'aux journaux.
 */
public class SteamException extends Exception {

    public SteamException(String message) {
        super(message);
    }
}
