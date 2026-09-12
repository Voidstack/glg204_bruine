package com.enosistudio.bruine.gacha.exception;

import lombok.Getter;

/**
 * Levée quand le joueur n'a pas assez de points pour l'action demandée.
 */
@Getter
public class InsufficientScoreException extends RuntimeException {
    private final int requiredScore;

    public InsufficientScoreException(int currentScore, int requiredScore) {
        super("Score insuffisant : " + currentScore + " point(s) pour un coût de " + requiredScore);
        this.requiredScore = requiredScore;
    }

}
