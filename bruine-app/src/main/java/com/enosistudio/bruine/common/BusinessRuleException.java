package com.enosistudio.bruine.common;

/**
 * Action refusée à l'utilisateur par une règle du jeu.
 * À distinguer d'une IllegalArgumentException, qui signale un défaut de programmation.
 * Ici le message est écrit pour être lu par le joueur, il lui est affiché tel quel par
 * {@link BusinessRuleExceptionHandler}.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
