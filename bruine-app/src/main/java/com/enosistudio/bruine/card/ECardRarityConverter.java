package com.enosistudio.bruine.card;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * La colonne {@code gacha_reward.rarity} stocke le code en minuscules ("common", "epic", ...),
 * ce convertisseur évite d'avoir à migrer ces valeurs pour passer l'entité en enum.
 */
@Converter(autoApply = true)
public class ECardRarityConverter implements AttributeConverter<ECardRarity, String> {

    @Override
    public String convertToDatabaseColumn(ECardRarity rarity) {
        return rarity == null ? null : rarity.getCode();
    }

    @Override
    public ECardRarity convertToEntityAttribute(String code) {
        return code == null ? null : ECardRarity.fromString(code);
    }
}
