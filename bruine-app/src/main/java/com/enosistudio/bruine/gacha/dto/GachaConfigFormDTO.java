package com.enosistudio.bruine.gacha.dto;

public record GachaConfigFormDTO(
        int rarityLegendary,
        int rarityEpic,
        int rarityRare,
        int rarityUncommon,
        int rarityCommon,

        int finishNegative,
        int finishPolychrome,
        int finishFoil,
        int finishHolographic,
        int finishNormal,

        int xpBaseCommon,
        int xpBaseUncommon,
        int xpBaseRare,
        int xpBaseEpic,
        int xpBaseLegendary,

        int xpMultNormal,
        int xpMultHolographic,
        int xpMultFoil,
        int xpMultNegative,
        int xpMultPolychrome) {
}
