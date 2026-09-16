package com.enosistudio.bruine.common;

/**
 * Refus faute de points : tirage au gacha comme achat au marché.
 */
public class InsufficientScoreException extends BusinessRuleException {

    public InsufficientScoreException(int requiredScore) {
        super("Score insuffisant, il vous faut " + requiredScore + " 💧.");
    }
}
