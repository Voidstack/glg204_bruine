package com.enosistudio.bruine.card;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
