package com.enosistudio.bruine.card;

import lombok.Getter;

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

    public static ECardRarity fromString(String code) {
        return valueOf(code.toUpperCase());
    }

    /**
     * Code en minuscules : valeur stockée en base et classe CSS de la carte.
     */
    public String getCode() {
        return name().toLowerCase();
    }
}
