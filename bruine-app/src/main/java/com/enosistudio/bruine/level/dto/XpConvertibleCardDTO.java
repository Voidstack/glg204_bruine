package com.enosistudio.bruine.level.dto;

import com.enosistudio.bruine.card.CardViewDTO;
import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.gacha.model.GachaReward;

/**
 * Un empilement de cartes de la collection, accompagné de l'expérience qu'une carte rapporte.
 */
public record XpConvertibleCardDTO(GachaReward reward, ECardFinish finish, long count, int xp) {
    public CardViewDTO view() {
        return CardViewDTO.of(reward, finish, count);
    }
}
