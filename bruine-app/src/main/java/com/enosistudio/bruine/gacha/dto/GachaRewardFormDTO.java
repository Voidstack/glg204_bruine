package com.enosistudio.bruine.gacha.dto;

import com.enosistudio.bruine.card.ECardRarity;

public record GachaRewardFormDTO(
        String emoji,
        String name,
        ECardRarity rarity,
        String description) {
}
