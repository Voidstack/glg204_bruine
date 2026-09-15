package com.enosistudio.bruine.card;

import com.enosistudio.bruine.gacha.model.GachaConfig;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Finitions d'une carte. {@link #NORMAL} est l'absence de finition : ni badge ni effet visuel.
 */
@Getter
public enum ECardFinish {
    NORMAL("⬜", "Normal", "NORM"), HOLOGRAPHIC("💠", "Holographique", "HOLO"), FOIL("✨", "Foil", "FOIL"), POLYCHROME("🌈", "Polychrome", "POLY"), NEGATIVE("☯", "Négatif", "NEG");

    private final String emoji;
    private final String label;
    private final String badge;

    ECardFinish(String emoji, String label, String badge) {
        this.emoji = emoji;
        this.label = label;
        this.badge = badge;
    }

    public static ECardFinish roll(GachaConfig config) {
        // pas vraiment safe il faudrais utiliser SecureRandom..
        int draw = ThreadLocalRandom.current().nextInt(Math.max(config.finishWeightTotal(), 1));
        ECardFinish[] finishes = values();
        for (int i = finishes.length - 1; i > 0; i--) {
            int weight = config.getFinishWeight(finishes[i]);
            if (draw < weight) return finishes[i];
            draw -= weight;
        }
        return NORMAL;
    }

    /**
     * Code en minuscules, classe CSS de la carte et identifiants côté JavaScript. c'est pas super.
     */
    public String getCode() {
        return name().toLowerCase();
    }
}
