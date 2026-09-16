package com.enosistudio.bruine.card;

import com.enosistudio.bruine.gacha.model.GachaConfig;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Raretés, de la plus commune à la plus rare : l'ordre de déclaration sert au tri.
 */
@Getter
public enum ECardRarity {
    COMMON("🪨", "#4a5568", "Commun"),
    UNCOMMON("🍃", "#68d391", "Peu commun"),
    RARE("💎", "#4299e1", "Rare"),
    EPIC("🔮", "#9b59b6", "Épique"),
    LEGENDARY("⭐", "#f6ad55", "Légendaire");

    private final String emoji;
    private final String color;
    private final String label;

    ECardRarity(String emoji, String color, String label) {
        this.emoji = emoji;
        this.color = color;
        this.label = label;
    }

    public static ECardRarity getRandomRarityFromConfig(GachaConfig config) {
        // pas vraiment safe il faudrais utiliser SecureRandom..
        int draw = ThreadLocalRandom.current().nextInt(Math.max(config.rarityWeightTotal(), 1));
        ECardRarity[] rarities = values();
        for (int i = rarities.length - 1; i > 0; i--) {
            int weight = config.rarityWeightFor(rarities[i]);
            if (draw < weight) return rarities[i];
            draw -= weight;
        }
        return COMMON;
    }

    public static ECardRarity fromString(String code) {
        return valueOf(code.toUpperCase());
    }

    /**
     * Code en minuscules, valeur stockée en base et classe CSS de la carte. c'est pas super.
     */
    public String getCode() {
        return name().toLowerCase();
    }
}
