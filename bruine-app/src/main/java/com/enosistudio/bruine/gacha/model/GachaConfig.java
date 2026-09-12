package com.enosistudio.bruine.gacha.model;

import com.enosistudio.bruine.card.ECardRarity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gacha_config")
@Getter
@Setter
public class GachaConfig {

    @Id
    private int id = 1;

    private int rarityLegendary;
    private int rarityEpic;
    private int rarityRare;
    private int rarityUncommon;
    private int rarityCommon;

    private int finishNegative;
    private int finishPolychrome;
    private int finishFoil;
    private int finishHolographic;
    private int finishNormal;

    private int xpBaseCommon = 10;
    private int xpBaseUncommon = 25;
    private int xpBaseRare = 75;
    private int xpBaseEpic = 200;
    private int xpBaseLegendary = 500;

    private int xpMultNormal = 1;
    private int xpMultHolographic = 2;
    private int xpMultFoil = 3;
    private int xpMultPolychrome = 4;
    private int xpMultNegative = 5;

    /**
     * XP rapportée par une carte de cette rareté, avant multiplicateur de finition.
     */
    public int xpBaseFor(ECardRarity rarity) {
        return switch (rarity) {
            case LEGENDARY -> xpBaseLegendary;
            case EPIC -> xpBaseEpic;
            case RARE -> xpBaseRare;
            case UNCOMMON -> xpBaseUncommon;
            case COMMON -> xpBaseCommon;
        };
    }
}
