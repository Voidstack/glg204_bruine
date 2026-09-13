package com.enosistudio.bruine.steam.exception;

/**
 * L'API Steam n'a pas répondu ce qu'on attendait : injoignable, code d'erreur HTTP,
 * JSON illisible, ou profil introuvable. Le message n'est destiné qu'aux journaux.
 * <p>
 * Volontairement vérifiée : la plupart des appelants ne veulent pas échouer mais
 * <em>dégrader</em> (avatar par défaut, compteurs inchangés, liste vide), et le
 * compilateur doit les forcer à trancher au cas par cas.
 */
public class SteamException extends Exception {

    public SteamException(String message) {
        super(message);
    }

    public SteamException(String message, Throwable cause) {
        super(message, cause);
    }
}
